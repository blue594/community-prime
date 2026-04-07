package com.community.prime.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.prime.entity.Voucher;
import com.community.prime.entity.VoucherOrder;
import com.community.prime.handler.BusinessException;
import com.community.prime.mapper.VoucherMapper;
import com.community.prime.mapper.VoucherOrderMapper;
import com.community.prime.service.VoucherService;
import com.community.prime.utils.RedisIdWorker;
import com.community.prime.utils.UserHolder;
import com.community.prime.vo.Result;
import com.community.prime.vo.VoucherVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.StreamOffset;
import static com.community.prime.utils.RedisConstants.*;

// todo
/**
 * 优惠券服务实现类 - 【面试重点】秒杀系统核心实现
 * 
 * 【面试点1】秒杀系统设计方案
 * - 库存预热：活动开始前将库存加载到Redis，避免数据库压力
 * - Redis预减库存：使用Lua脚本保证原子性，避免超卖
 * - 异步下单：使用Redis Stream消息队列削峰填谷
 * - 分布式锁：Redisson保证一人一单，防止重复抢购
 * 
 * 【面试点2】Redis分布式锁
 * - 使用Redisson实现，支持可重入、自动续期（Watch Dog机制）
 * - 锁粒度：用户+优惠券维度，细粒度提高并发性能
 * 
 * 【面试点3】消息队列应用
 * - Redis Stream实现异步订单创建，降低接口响应时间
 * - 消费者组模式，支持消息确认和重试机制
 * 
 * 【面试点4】事务管理
 * - @Transactional保证扣减库存和创建订单的原子性
 * - 库存扣减使用乐观锁（WHERE stock > 0）防止超卖
 */
@Slf4j
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements VoucherService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    // 秒杀Lua脚本：预减库存
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 初始化：创建Redis Stream消费者组
     */
    @PostConstruct
    public void init() {
        try {
            // 尝试创建Stream和消费者组
            stringRedisTemplate.opsForStream().createGroup(SECKILL_ORDER_KEY, ReadOffset.from("0"), "orderGroup");
        } catch (Exception e) {
            // Stream已存在，忽略异常
            log.info("Redis Stream已存在，无需创建");
        }

        // 启动订单处理线程
        startOrderConsumer();
    }

    @Override
    public Result addSeckillVoucher(Voucher voucher) {
        // 1. 保存优惠券到数据库
        voucher.setType(1); // 秒杀券
        save(voucher);

        // 2. 将库存预热到Redis
        stringRedisTemplate.opsForValue().set(
                SECKILL_STOCK_KEY + voucher.getId(),
                voucher.getStock().toString()
        );

        log.info("秒杀券添加成功，ID：{}，库存：{}", voucher.getId(), voucher.getStock());
        return Result.ok(voucher.getId());
    }

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<Voucher> vouchers = baseMapper.selectListByShopId(shopId);
        List<VoucherVO> voList = vouchers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.ok(voList);
    }

    @Override
    public Result querySeckillVoucher(Long voucherId) {
        Voucher voucher = getById(voucherId);
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        }

        VoucherVO vo = convertToVO(voucher);

        // 从Redis获取实时库存
        String stockStr = stringRedisTemplate.opsForValue().get(SECKILL_STOCK_KEY + voucherId);
        if (stockStr != null) {
            vo.setStock(Integer.parseInt(stockStr));
        }

        // 计算状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) {
            vo.setStatus(0); // 未开始
        } else if (now.isAfter(voucher.getEndTime())) {
            vo.setStatus(2); // 已结束
        } else {
            vo.setStatus(1); // 进行中
        }

        return Result.ok(vo);
    }

    // todo
    /**
     * 【面试重点】秒杀下单核心逻辑
     * 
     * 【面试点】如何防止超卖？
     * 1. Redis预减库存：使用Lua脚本保证原子性
     * 2. 数据库乐观锁：UPDATE tb_voucher SET stock = stock - 1 WHERE stock > 0
     * 
     * 【面试点】如何保证一人一单？
     * 1. Redisson分布式锁：锁粒度为用户+优惠券
     * 2. 数据库唯一索引：uk_user_voucher(user_id, voucher_id)
     * 
     * 【面试点】如何优化秒杀性能？
     * 1. 库存预热：提前加载到Redis
     * 2. 异步下单：Redis Stream消息队列削峰
     * 3. 接口限流：可扩展Sentinel限流
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询优惠券信息
        Voucher voucher = getById(voucherId);
        if (voucher == null) {
            throw new BusinessException("优惠券不存在");
        }

        // 2. 判断秒杀是否开始
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) {
            throw new BusinessException("秒杀尚未开始");
        }
        if (now.isAfter(voucher.getEndTime())) {
            throw new BusinessException("秒杀已结束");
        }

        // 3. 【面试点】Redis + Lua脚本预减库存（原子操作，防止超卖）
        // 先检查Redis中是否有库存key，如果没有从数据库加载
        String stockKey = SECKILL_STOCK_KEY + voucherId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        if (stockStr == null) {
            // Redis中没有库存，从数据库加载并预热到Redis
            Integer dbStock = voucher.getStock();
            if (dbStock != null && dbStock > 0) {
                stringRedisTemplate.opsForValue().set(stockKey, dbStock.toString());
            } else {
                throw new BusinessException("库存不足");
            }
        }

        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.singletonList(stockKey)
        );

        // 4. 判断结果
        if (result == null || result < 0) {
            // 库存不足
            throw new BusinessException("库存不足");
        }

        // 5. 【面试点】Redisson分布式锁保证一人一单
        String lockKey = LOCK_SECKILL_KEY + voucherId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，最多等待1秒，锁有效期10秒
            boolean isLock = lock.tryLock(1, 10, java.util.concurrent.TimeUnit.SECONDS);
            if (!isLock) {
                // 恢复库存
                stringRedisTemplate.opsForValue().increment(SECKILL_STOCK_KEY + voucherId);
                throw new BusinessException("请勿重复抢购");
            }

            try {
                // 6. Double Check：再次检查是否已购买
                Long existOrderId = voucherOrderMapper.selectOrderIdByUserAndVoucher(userId, voucherId);
                if (existOrderId != null) {
                    // 恢复库存
                    stringRedisTemplate.opsForValue().increment(SECKILL_STOCK_KEY + voucherId);
                    throw new BusinessException("每人限购一单");
                }

                // 7. 生成订单ID（使用Redis分布式ID）
                Long orderId = redisIdWorker.nextId("voucher_order");

                // 8. 【面试点】发送消息到Redis Stream，异步创建订单（削峰填谷）
                Map<String, String> message = new HashMap<>();
                message.put("voucherId", voucherId.toString());
                message.put("userId", userId.toString());
                message.put("orderId", orderId.toString());

                stringRedisTemplate.opsForStream().add(SECKILL_ORDER_KEY, message);

                log.info("用户{}秒杀成功，订单号：{}，优惠券：{}", userId, orderId, voucherId);
                return Result.ok(orderId);

            } finally {
                lock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(Long voucherId, Long userId, Long orderId) {
        // 1. 扣减数据库库存
        int affected = baseMapper.deductStock(voucherId);
        if (affected == 0) {
            log.error("扣减库存失败，优惠券：{}，用户：{}", voucherId, userId);
            return;
        }

        // 2. 创建订单
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        order.setStatus(0); // 未支付

        voucherOrderMapper.insert(order);

        log.info("订单创建成功，订单号：{}，用户：{}，优惠券：{}", orderId, userId, voucherId);
    }

    @Override
    public Result queryMyVoucherOrders() {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 使用 QueryWrapper 查询
        QueryWrapper<VoucherOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        List<VoucherOrder> orders = voucherOrderMapper.selectList(wrapper);

        return Result.ok(orders);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result payVoucherOrder(Long orderId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        VoucherOrder order = voucherOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许支付");
        }

        // 模拟支付成功
        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 生成核销码（6位随机数字）
        String verifyCode = RandomUtil.randomNumbers(6);
        order.setVerifyCode(verifyCode);

        voucherOrderMapper.updateById(order);

        log.info("用户{}支付优惠券订单成功，订单号：{}，核销码：{}", userId, orderId, verifyCode);
        return Result.ok(verifyCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result verifyVoucher(Long orderId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        VoucherOrder order = voucherOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("只有已支付的优惠券才能核销");
        }

        // 核销
        order.setStatus(2); // 已核销
        order.setUseTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        voucherOrderMapper.updateById(order);

        log.info("用户{}核销优惠券成功，订单号：{}", userId, orderId);
        return Result.ok("核销成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result buyVoucher(Long voucherId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询优惠券
        Voucher voucher = getById(voucherId);
        if (voucher == null) {
            throw new BusinessException("优惠券不存在");
        }

        // 2. 检查是否是普通优惠券
        if (voucher.getType() != 0) {
            throw new BusinessException("秒杀券请通过秒杀通道购买");
        }

        // 3. 检查库存
        if (voucher.getStock() == null || voucher.getStock() <= 0) {
            throw new BusinessException("优惠券已售罄");
        }

        // 4. 检查有效期
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getBeginTime() != null && now.isBefore(voucher.getBeginTime())) {
            throw new BusinessException("优惠券尚未开售");
        }
        if (voucher.getEndTime() != null && now.isAfter(voucher.getEndTime())) {
            throw new BusinessException("优惠券已过期");
        }

        // 5. 一人一单校验
        Long existOrderId = voucherOrderMapper.selectOrderIdByUserAndVoucher(userId, voucherId);
        if (existOrderId != null) {
            throw new BusinessException("每人限购一张");
        }

        // 6. 扣减库存（乐观锁）
        int affected = baseMapper.deductStock(voucherId);
        if (affected == 0) {
            throw new BusinessException("库存不足");
        }

        // 7. 创建订单
        Long orderId = redisIdWorker.nextId("voucher_order");
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        order.setStatus(0); // 未支付
        voucherOrderMapper.insert(order);

        log.info("用户{}购买优惠券成功，订单号：{}，优惠券：{}", userId, orderId, voucherId);
        return Result.ok(orderId);
    }

    @Override
    public Result queryNormalVouchers(Long shopId) {
        List<Voucher> vouchers = baseMapper.selectNormalVouchersByShopId(shopId);
        List<VoucherVO> voList = vouchers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.ok(voList);
    }

    /**
     * 启动订单消费者线程
     */
    private void startOrderConsumer() {
        new Thread(() -> {
            while (true) {
                try {
                    // 使用简单的读取方式
                    List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                            StreamOffset.fromStart(SECKILL_ORDER_KEY)
                    );

                    if (records == null || records.isEmpty()) {
                        Thread.sleep(1000);
                        continue;
                    }

                    // 处理消息
                    for (MapRecord<String, Object, Object> record : records) {
                        Map<Object, Object> value = record.getValue();
                        Long voucherId = Long.valueOf(value.get("voucherId").toString());
                        Long userId = Long.valueOf(value.get("userId").toString());
                        Long orderId = Long.valueOf(value.get("orderId").toString());

                        try {
                            // 创建订单
                            createVoucherOrder(voucherId, userId, orderId);
                            // 确认消息（删除已处理的消息）
                            stringRedisTemplate.opsForStream().delete(SECKILL_ORDER_KEY, record.getId());
                        } catch (Exception e) {
                            log.error("处理订单消息失败", e);
                        }
                    }

                } catch (Exception e) {
                    log.error("订单消费异常", e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "order-consumer").start();
    }

    /**
     * 转换为VO
     */
    private VoucherVO convertToVO(Voucher voucher) {
        VoucherVO vo = new VoucherVO();
        BeanUtil.copyProperties(voucher, vo);
        return vo;
    }
}
