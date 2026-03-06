package com.community.prime.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.prime.dto.ProductOrderDTO;
import com.community.prime.entity.Product;
import com.community.prime.entity.ProductOrder;
import com.community.prime.entity.SeckillProduct;
import com.community.prime.handler.BusinessException;
import com.community.prime.mapper.ProductMapper;
import com.community.prime.mapper.ProductOrderMapper;
import com.community.prime.mapper.SeckillProductMapper;
import com.community.prime.service.ProductService;
import com.community.prime.utils.CacheClient;
import com.community.prime.utils.RedisIdWorker;
import com.community.prime.utils.UserHolder;
import com.community.prime.vo.PageResult;
import com.community.prime.vo.ProductVO;
import com.community.prime.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.community.prime.utils.RedisConstants.*;

/**
 * 【面试重点】商品服务实现类 - 缓存应用与订单事务管理
 * 
 * 【面试点 1】缓存策略的实际应用
 * - 使用 CacheClient 封装缓存操作，统一处理缓存三剑客问题
 * - 商品详情查询使用互斥锁解决缓存击穿
 * 
 * 【面试点 2】事务管理
 * - @Transactional 保证扣减库存和创建订单的原子性
 * - rollbackFor = Exception.class 确保所有异常都回滚
 * 
 * 【面试点 3】乐观锁防超卖
 * - 数据库层面：UPDATE tb_product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}
 * - 通过 affected rows 判断是否扣减成功
 * 
 * 【面试点 4】缓存与数据库一致性
 * - 写操作（创建订单、取消订单）后删除缓存，而非更新缓存
 * - 删除缓存策略更简单，避免并发更新导致的脏数据
 */
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private ProductOrderMapper productOrderMapper;

    @Resource
    private SeckillProductMapper seckillProductMapper;

    @Override
    public Result queryProductList(Integer current, Integer size) {
        // 1. 分页查询 - 只返回上架商品（用户端用）
        Page<Product> page = query()
                .eq("status", 1)
                .orderByDesc("sold")
                .page(new Page<>(current, size));

        // 2. 封装VO
        List<ProductVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.ok(new PageResult(page.getTotal(), voList));
    }

    @Override
    public Result queryAllProductList(Integer current, Integer size) {
        // 1. 分页查询 - 返回所有商品（管理员用）
        Page<Product> page = query()
                .orderByDesc("create_time")
                .page(new Page<>(current, size));

        // 2. 封装VO
        List<ProductVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.ok(new PageResult(page.getTotal(), voList));
    }

    /**
     * 【面试点】缓存击穿实际应用
     * 
     * 使用 queryWithMutex 方法：
     * 1. 先查缓存，命中直接返回
     * 2. 未命中获取互斥锁
     * 3. 获取锁后 Double Check
     * 4. 查数据库并写入缓存
     * 5. 释放锁
     */
    @Override
    public Result queryProductById(Long id) {
        // 使用互斥锁解决缓存击穿
        ProductVO productVO = cacheClient.queryWithMutex(
                PRODUCT_CACHE_KEY, id, ProductVO.class,
                this::queryProductFromDb,
                PRODUCT_CACHE_TTL, TimeUnit.MINUTES
        );

        if (productVO == null) {
            return Result.fail("商品不存在");
        }

        return Result.ok(productVO);
    }

    /**
     * 从数据库查询商品（供缓存使用）
     */
    private ProductVO queryProductFromDb(Long id) {
        Product product = getById(id);
        if (product == null || product.getDeleted() == 1) {
            return null;
        }
        return convertToVO(product);
    }

    @Override
    public Result searchProduct(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.fail("搜索关键词不能为空");
        }

        // 使用索引优化查询
        List<Product> products = baseMapper.searchByKeyword(keyword.trim());
        List<ProductVO> voList = products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.ok(voList);
    }

    /**
     * 【面试重点】创建订单 - 事务与乐观锁
     * 
     * 【面试点】事务管理要点：
     * 1. @Transactional 保证扣减库存和创建订单的原子性
     * 2. rollbackFor = Exception.class 确保所有异常都触发回滚
     * 3. 事务内避免远程调用，防止大事务影响性能
     * 
     * 【面试点】乐观锁防超卖实现：
     * 1. SQL: UPDATE tb_product SET stock = stock - #{quantity}, sold = sold + #{quantity} 
     *         WHERE id = #{id} AND stock >= #{quantity}
     * 2. 通过 affected rows 判断是否扣减成功
     * 3. 失败则抛出异常，事务回滚
     * 
     * 【面试点】缓存一致性策略：
     * - 采用 Cache Aside 模式：先更新数据库，再删除缓存
     * - 不更新缓存是因为并发环境下更新可能导致脏数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(ProductOrderDTO productOrderDTO) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Long productId = productOrderDTO.getProductId();
        Integer quantity = productOrderDTO.getQuantity();

        // 1. 查询商品
        Product product = getById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }

        // 2. 校验库存
        if (product.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }

        // 【面试点】3. 扣减库存（乐观锁防止超卖）
        // SQL: UPDATE tb_product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}
        int affected = baseMapper.deductStock(productId, quantity);
        if (affected == 0) {
            throw new BusinessException("库存不足，请稍后重试");
        }

        // 4. 创建订单
        ProductOrder order = new ProductOrder();
        order.setId(redisIdWorker.nextId("product_order"));
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);

        // 计算金额
        BigDecimal totalAmount = product.getPrice().multiply(new BigDecimal(quantity));
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);

        order.setAddress(productOrderDTO.getAddress());
        order.setPhone(productOrderDTO.getPhone());
        order.setStatus(0); // 待付款

        productOrderMapper.insert(order);

        // 【面试点】5. 删除商品缓存（Cache Aside 模式保证数据一致性）
        stringRedisTemplate.delete(PRODUCT_CACHE_KEY + productId);

        log.info("用户{}创建订单成功，订单号：{}", userId, order.getId());
        return Result.ok(order.getId());
    }

    @Override
    public Result queryOrderList(Integer status, Integer current, Integer size) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 使用优化版本
        IPage<ProductOrder> page = new Page<>(current, size);
        IPage<ProductOrder> resultPage = productOrderMapper.selectOrderPage(page, userId, status);
        return Result.ok(new PageResult(resultPage.getTotal(), resultPage.getRecords()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelOrder(Long orderId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询订单
        ProductOrder order = productOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. 校验订单归属
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        // 3. 校验订单状态（只能取消待付款订单）
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许取消");
        }

        // 4. 更新订单状态
        order.setStatus(4); // 已取消
        order.setUpdateTime(LocalDateTime.now());
        productOrderMapper.updateById(order);

        // 5. 恢复库存
        Product product = getById(order.getProductId());
        if (product != null) {
            product.setStock(product.getStock() + order.getQuantity());
            product.setSold(product.getSold() - order.getQuantity());
            updateById(product);

            // 删除缓存
            stringRedisTemplate.delete(PRODUCT_CACHE_KEY + product.getId());
        }

        log.info("用户{}取消订单成功，订单号：{}", userId, orderId);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result payOrder(Long orderId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询订单
        ProductOrder order = productOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. 校验订单归属
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        // 3. 校验订单状态（只能支付待付款订单）
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许支付");
        }

        // 4. 模拟支付成功，更新订单状态
        order.setStatus(1); // 待发货
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        productOrderMapper.updateById(order);

        log.info("用户{}支付订单成功，订单号：{}", userId, orderId);
        return Result.ok("支付成功");
    }

    @Override
    public Result querySeckillProductList() {
        List<SeckillProduct> list = seckillProductMapper.selectSeckillProductList();
        return Result.ok(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result seckillProduct(Long seckillProductId, String address, String phone) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询秒杀商品
        SeckillProduct seckillProduct = seckillProductMapper.selectSeckillProductById(seckillProductId);
        if (seckillProduct == null) {
            throw new BusinessException("秒杀商品不存在");
        }

        // 2. 判断秒杀是否开始
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillProduct.getBeginTime())) {
            throw new BusinessException("秒杀尚未开始");
        }
        if (now.isAfter(seckillProduct.getEndTime())) {
            throw new BusinessException("秒杀已结束");
        }

        // 3. 检查库存
        if (seckillProduct.getStock() <= 0) {
            throw new BusinessException("库存不足");
        }

        // 4. 扣减秒杀库存（乐观锁）
        int affected = seckillProductMapper.deductStock(seckillProductId);
        if (affected == 0) {
            throw new BusinessException("库存不足，手慢了");
        }

        // 5. 创建商品订单
        ProductOrder order = new ProductOrder();
        order.setId(redisIdWorker.nextId("product_order"));
        order.setUserId(userId);
        order.setProductId(seckillProduct.getProductId());
        order.setQuantity(1);
        order.setTotalAmount(seckillProduct.getSeckillPrice());
        order.setPayAmount(seckillProduct.getSeckillPrice());
        order.setAddress(address);
        order.setPhone(phone);
        order.setStatus(0); // 待付款
        productOrderMapper.insert(order);

        log.info("用户{}秒杀商品成功，订单号：{}，商品：{}", userId, order.getId(), seckillProduct.getProductId());
        return Result.ok(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result confirmReceipt(Long orderId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询订单
        ProductOrder order = productOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. 校验订单归属
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        // 3. 校验订单状态（只能确认待收货订单）
        if (order.getStatus() != 2) {
            throw new BusinessException("订单状态不允许确认收货");
        }

        // 4. 更新订单状态为已完成
        order.setStatus(3); // 已完成
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        productOrderMapper.updateById(order);

        log.info("用户{}确认收货成功，订单号：{}", userId, orderId);
        return Result.ok("确认收货成功");
    }

    /**
     * 转换为VO
     */
    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setImage(product.getImage());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setSold(product.getSold());
        vo.setDescription(product.getDescription());
        vo.setStatus(product.getStatus());
        return vo;
    }
}
