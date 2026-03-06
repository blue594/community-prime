package com.community.prime.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.prime.entity.Admin;
import com.community.prime.entity.ProductOrder;
import com.community.prime.entity.ServiceBooking;
import com.community.prime.entity.User;
import com.community.prime.entity.VoucherOrder;
import com.community.prime.handler.BusinessException;
import com.community.prime.mapper.AdminMapper;
import com.community.prime.mapper.ProductMapper;
import com.community.prime.mapper.ProductOrderMapper;
import com.community.prime.mapper.ServiceBookingMapper;
import com.community.prime.mapper.UserMapper;
import com.community.prime.mapper.VoucherOrderMapper;
import com.community.prime.service.AdminService;
import com.community.prime.utils.AdminHolder;
import com.community.prime.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.community.prime.utils.RedisConstants.*;

/**
 * 管理员服务实现类
 */
@Slf4j
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductOrderMapper productOrderMapper;

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private ServiceBookingMapper serviceBookingMapper;

    @Override
    public Result login(String username, String password, HttpSession session) {
        // 1. 校验参数
        if (username == null || username.trim().isEmpty()) {
            return Result.fail("账号不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.fail("密码不能为空");
        }

        // 2. 查询管理员
        Admin admin = query().eq("username", username.trim()).one();
        if (admin == null) {
            return Result.fail("账号或密码错误");
        }

        // 3. 校验密码
        String encryptedPassword = DigestUtil.md5Hex(password);
        if (!encryptedPassword.equals(admin.getPassword())) {
            return Result.fail("账号或密码错误");
        }

        // 4. 校验状态
        if (admin.getStatus() != 1) {
            return Result.fail("账号已被禁用");
        }

        // 5. 生成token并保存到Redis
        String token = UUID.randomUUID().toString();
        String tokenKey = LOGIN_ADMIN_KEY + token;

        // 将管理员信息转为Map存储（脱敏）
        Map<String, Object> adminMap = new HashMap<>();
        adminMap.put("id", admin.getId().toString());
        adminMap.put("username", admin.getUsername());
        adminMap.put("name", admin.getName());
        adminMap.put("role", admin.getRole().toString());
        adminMap.put("icon", admin.getIcon() != null ? admin.getIcon() : "");

        stringRedisTemplate.opsForHash().putAll(tokenKey, adminMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_ADMIN_TTL, TimeUnit.MINUTES);

        // 6. 返回token和管理员信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("id", admin.getId());
        result.put("username", admin.getUsername());
        result.put("name", admin.getName());
        result.put("role", admin.getRole());
        result.put("icon", admin.getIcon());

        log.info("管理员登录成功：{}", username);
        return Result.ok(result);
    }

    @Override
    public Result getCurrentAdmin() {
        Admin admin = AdminHolder.getAdmin();
        if (admin == null) {
            throw new BusinessException(401, "未登录");
        }
        // 脱敏返回
        admin.setPassword(null);
        return Result.ok(admin);
    }

    @Override
    public Result logout() {
        // 从ThreadLocal获取token并删除Redis中的登录状态
        String token = AdminHolder.getToken();
        if (token != null) {
            stringRedisTemplate.delete(LOGIN_ADMIN_KEY + token);
        }
        AdminHolder.removeAdmin();
        return Result.ok("退出成功");
    }

    @Override
    public Result changePassword(String oldPassword, String newPassword) {
        Admin admin = AdminHolder.getAdmin();
        if (admin == null) {
            throw new BusinessException(401, "未登录");
        }

        // 校验旧密码
        String encryptedOldPassword = DigestUtil.md5Hex(oldPassword);
        Admin dbAdmin = getById(admin.getId());
        if (!encryptedOldPassword.equals(dbAdmin.getPassword())) {
            return Result.fail("旧密码错误");
        }

        // 校验新密码
        if (newPassword == null || newPassword.length() < 6) {
            return Result.fail("新密码长度不能少于6位");
        }

        // 更新密码
        dbAdmin.setPassword(DigestUtil.md5Hex(newPassword));
        updateById(dbAdmin);

        return Result.ok("密码修改成功");
    }

    @Override
    public Result createAdmin(Admin admin) {
        // 校验账号
        if (admin.getUsername() == null || admin.getUsername().trim().isEmpty()) {
            return Result.fail("账号不能为空");
        }
        if (admin.getUsername().length() < 4 || admin.getUsername().length() > 20) {
            return Result.fail("账号长度应为4-20位");
        }

        // 检查账号是否已存在
        Admin existAdmin = query().eq("username", admin.getUsername().trim()).one();
        if (existAdmin != null) {
            return Result.fail("账号已存在");
        }

        // 设置默认密码
        String defaultPassword = admin.getPassword();
        if (defaultPassword == null || defaultPassword.isEmpty()) {
            defaultPassword = "123456";
        }
        admin.setPassword(DigestUtil.md5Hex(defaultPassword));

        // 设置默认状态
        if (admin.getRole() == null) {
            admin.setRole(2); // 默认普通管理员
        }
        if (admin.getStatus() == null) {
            admin.setStatus(1); // 默认启用
        }

        save(admin);
        log.info("创建管理员成功：{}", admin.getUsername());

        // 脱敏返回
        admin.setPassword(null);
        return Result.ok(admin);
    }

    @Override
    public Result updateAdmin(Admin admin) {
        if (admin.getId() == null) {
            return Result.fail("管理员ID不能为空");
        }

        // 不允许修改密码和账号
        admin.setPassword(null);
        admin.setUsername(null);

        updateById(admin);
        return Result.ok("更新成功");
    }

    @Override
    public Result deleteAdmin(Long id) {
        Admin currentAdmin = AdminHolder.getAdmin();
        if (currentAdmin == null || currentAdmin.getRole() != 1) {
            throw new BusinessException(403, "无权限操作");
        }

        // 不能删除自己
        if (id.equals(currentAdmin.getId())) {
            return Result.fail("不能删除当前登录账号");
        }

        removeById(id);
        return Result.ok("删除成功");
    }

    @Override
    public Result queryUserList(Integer current, Integer size) {
        Page<User> page = new Page<>(current, size);
        Page<User> resultPage = userMapper.selectPage(page, null);
        return Result.ok(new com.community.prime.vo.PageResult(resultPage.getTotal(), resultPage.getRecords()));
    }

    @Override
    public Result deleteUser(Long id) {
        userMapper.deleteById(id);
        return Result.ok("删除成功");
    }

    @Override
    public Result queryProductOrderList(Integer current, Integer size) {
        Page<ProductOrder> page = new Page<>(current, size);
        Page<ProductOrder> resultPage = productOrderMapper.selectPage(page, null);
        return Result.ok(new com.community.prime.vo.PageResult(resultPage.getTotal(), resultPage.getRecords()));
    }

    @Override
    public Result queryVoucherOrderList(Integer current, Integer size) {
        Page<VoucherOrder> page = new Page<>(current, size);
        Page<VoucherOrder> resultPage = voucherOrderMapper.selectPage(page, null);
        return Result.ok(new com.community.prime.vo.PageResult(resultPage.getTotal(), resultPage.getRecords()));
    }

    @Override
    public Result shipProductOrder(Long orderId) {
        ProductOrder order = productOrderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() != 1) {
            return Result.fail("订单状态不允许发货");
        }
        order.setStatus(2); // 待收货
        order.setDeliveryTime(java.time.LocalDateTime.now());
        productOrderMapper.updateById(order);
        return Result.ok("发货成功");
    }

    @Override
    public Result queryBookingList(Integer current, Integer size) {
        Page<ServiceBooking> page = new Page<>(current, size);
        Page<ServiceBooking> resultPage = serviceBookingMapper.selectPage(page, null);
        return Result.ok(new com.community.prime.vo.PageResult(resultPage.getTotal(), resultPage.getRecords()));
    }

    @Override
    public Result updateBookingStatus(Long id, Integer status) {
        ServiceBooking booking = serviceBookingMapper.selectById(id);
        if (booking == null) {
            return Result.fail("预约不存在");
        }
        booking.setStatus(status);
        serviceBookingMapper.updateById(booking);
        return Result.ok("状态更新成功");
    }

    @Override
    public Result cancelBooking(Long id) {
        ServiceBooking booking = serviceBookingMapper.selectById(id);
        if (booking == null) {
            return Result.fail("预约不存在");
        }
        booking.setStatus(4); // 已取消
        serviceBookingMapper.updateById(booking);
        return Result.ok("取消成功");
    }

    @Override
    public Result getDashboardStats() {
        // 查询商品数量
        Long productCount = productMapper.selectCount(null);

        // 查询订单数量（商品订单 + 优惠券订单）
        Long productOrderCount = productOrderMapper.selectCount(null);
        Long voucherOrderCount = voucherOrderMapper.selectCount(null);
        Long orderCount = productOrderCount + voucherOrderCount;

        // 查询用户数量
        Long userCount = userMapper.selectCount(null);

        // 查询预约数量
        Long bookingCount = serviceBookingMapper.selectCount(null);

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("productCount", productCount);
        stats.put("orderCount", orderCount);
        stats.put("userCount", userCount);
        stats.put("bookingCount", bookingCount);

        return Result.ok(stats);
    }

    @Override
    public Result updateIcon(String icon) {
        // 1. 获取当前登录管理员
        Admin admin = AdminHolder.getAdmin();
        if (admin == null) {
            throw new BusinessException(401, "未登录");
        }

        // 2. 更新头像
        Admin dbAdmin = getById(admin.getId());
        if (dbAdmin == null) {
            return Result.fail("管理员不存在");
        }
        dbAdmin.setIcon(icon);
        updateById(dbAdmin);

        // 3. 更新Redis中的管理员信息
        String token = AdminHolder.getToken();
        if (token != null) {
            String tokenKey = LOGIN_ADMIN_KEY + token;
            stringRedisTemplate.opsForHash().put(tokenKey, "icon", icon);
        }

        // 4. 更新ThreadLocal中的管理员信息
        admin.setIcon(icon);

        log.info("管理员{}更新头像成功", admin.getId());
        // 返回更新后的管理员信息
        return Result.ok(admin);
    }

    @Override
    public Result updateName(String name) {
        // 1. 获取当前登录管理员
        Admin admin = AdminHolder.getAdmin();
        if (admin == null) {
            throw new BusinessException(401, "未登录");
        }

        // 2. 校验姓名
        if (name == null || name.trim().isEmpty()) {
            return Result.fail("姓名不能为空");
        }
        if (name.length() > 50) {
            return Result.fail("姓名长度不能超过50个字符");
        }

        // 3. 更新姓名
        Admin dbAdmin = getById(admin.getId());
        if (dbAdmin == null) {
            return Result.fail("管理员不存在");
        }
        dbAdmin.setName(name.trim());
        updateById(dbAdmin);

        // 4. 更新Redis中的管理员信息
        String token = AdminHolder.getToken();
        if (token != null) {
            String tokenKey = LOGIN_ADMIN_KEY + token;
            stringRedisTemplate.opsForHash().put(tokenKey, "name", name.trim());
        }

        // 5. 更新ThreadLocal中的管理员信息
        admin.setName(name.trim());

        log.info("管理员{}更新姓名成功: {}", admin.getId(), name.trim());
        // 返回更新后的管理员信息
        return Result.ok(admin);
    }
}
