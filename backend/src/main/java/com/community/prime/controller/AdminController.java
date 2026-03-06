package com.community.prime.controller;

import com.community.prime.entity.Admin;
import com.community.prime.entity.Product;
import com.community.prime.entity.ServiceType;
import com.community.prime.entity.Voucher;
import com.community.prime.mapper.ServiceTypeMapper;
import com.community.prime.mapper.VoucherMapper;
import com.community.prime.service.AdminService;
import com.community.prime.service.ProductService;
import com.community.prime.service.ServiceTypeService;
import com.community.prime.service.VoucherService;
import com.community.prime.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private ProductService productService;

    @Resource
    private ServiceTypeService serviceTypeService;

    @Resource
    private ServiceTypeMapper serviceTypeMapper;

    @Resource
    private VoucherService voucherService;

    @Resource
    private VoucherMapper voucherMapper;

    // ==================== 管理员登录相关 ====================

    /**
     * 管理员登录 - 不需要认证
     */
    @PostMapping("/login")
    public Result login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session) {
        log.info("管理员登录请求: username={}", username);
        return adminService.login(username, password, session);
    }

    /**
     * 获取当前登录管理员
     */
    @GetMapping("/me")
    public Result me() {
        return adminService.getCurrentAdmin();
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result logout() {
        return adminService.logout();
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result changePassword(@RequestBody Map<String, String> params) {
        return adminService.changePassword(params.get("oldPassword"), params.get("newPassword"));
    }

    // ==================== 管理员管理 ====================

    /**
     * 创建管理员
     */
    @PostMapping("/create")
    public Result createAdmin(@RequestBody Admin admin) {
        return adminService.createAdmin(admin);
    }

    /**
     * 更新管理员
     */
    @PutMapping("/update")
    public Result updateAdmin(@RequestBody Admin admin) {
        return adminService.updateAdmin(admin);
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteAdmin(@PathVariable Long id) {
        return adminService.deleteAdmin(id);
    }

    /**
     * 获取管理员列表
     */
    @GetMapping("/list")
    public Result listAdmins() {
        return Result.ok(adminService.list());
    }

    // ==================== 商品管理 ====================

    /**
     * 查询所有商品列表（管理员用）
     */
    @GetMapping("/product/list")
    public Result listProducts(@RequestParam(defaultValue = "1") Integer current,
                               @RequestParam(defaultValue = "10") Integer size) {
        return productService.queryAllProductList(current, size);
    }

    /**
     * 添加商品
     */
    @PostMapping("/product/add")
    public Result addProduct(@RequestBody Product product) {
        productService.save(product);
        return Result.ok("添加成功");
    }

    /**
     * 更新商品
     */
    @PutMapping("/product/update")
    public Result updateProduct(@RequestBody Product product) {
        productService.updateById(product);
        return Result.ok("更新成功");
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/product/delete/{id}")
    public Result deleteProduct(@PathVariable Long id) {
        productService.removeById(id);
        return Result.ok("删除成功");
    }

    /**
     * 上传商品图片
     */
    @PostMapping("/product/upload-image")
    public Result uploadProductImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("请选择图片");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + suffix;

            String uploadDir = System.getProperty("user.dir") + "/uploads/product/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(uploadDir + filename);
            file.transferTo(dest);

            String url = "/uploads/product/" + filename;
            return Result.ok(url);

        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.fail("图片上传失败");
        }
    }

    // ==================== 家政服务类型管理 ====================

    /**
     * 添加服务类型
     */
    @PostMapping("/service-type/add")
    public Result addServiceType(@RequestBody ServiceType serviceType) {
        serviceTypeService.save(serviceType);
        return Result.ok("添加成功");
    }

    /**
     * 更新服务类型
     */
    @PutMapping("/service-type/update")
    public Result updateServiceType(@RequestBody ServiceType serviceType) {
        serviceTypeService.updateById(serviceType);
        return Result.ok("更新成功");
    }

    /**
     * 删除服务类型
     */
    @DeleteMapping("/service-type/delete/{id}")
    public Result deleteServiceType(@PathVariable Long id) {
        serviceTypeService.removeById(id);
        return Result.ok("删除成功");
    }

    /**
     * 查询所有服务类型列表（管理员用）
     */
    @GetMapping("/service-type/list")
    public Result listServiceTypes() {
        return Result.ok(serviceTypeMapper.selectAllList());
    }

    /**
     * 上传服务类型图片
     */
    @PostMapping("/service-type/upload-image")
    public Result uploadServiceTypeImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("请选择图片");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + suffix;

            String uploadDir = System.getProperty("user.dir") + "/uploads/service/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(uploadDir + filename);
            file.transferTo(dest);

            String url = "/uploads/service/" + filename;
            return Result.ok(url);

        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.fail("图片上传失败");
        }
    }

    // ==================== 优惠券管理 ====================

    /**
     * 查询所有优惠券列表（管理员用）
     */
    @GetMapping("/voucher/list")
    public Result listVouchers() {
        return Result.ok(voucherMapper.selectAllList());
    }

    /**
     * 添加优惠券
     */
    @PostMapping("/voucher/add")
    public Result addVoucher(@RequestBody Voucher voucher) {
        // 如果没有设置shopId，默认为1
        if (voucher.getShopId() == null) {
            voucher.setShopId(1L);
        }
        voucherService.save(voucher);
        return Result.ok("添加成功");
    }

    /**
     * 更新优惠券
     */
    @PutMapping("/voucher/update")
    public Result updateVoucher(@RequestBody Voucher voucher) {
        voucherService.updateById(voucher);
        return Result.ok("更新成功");
    }

    /**
     * 删除优惠券
     */
    @DeleteMapping("/voucher/delete/{id}")
    public Result deleteVoucher(@PathVariable Long id) {
        voucherService.removeById(id);
        return Result.ok("删除成功");
    }

    // ==================== 用户管理 ====================

    /**
     * 查询用户列表
     */
    @GetMapping("/user/list")
    public Result listUsers(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size) {
        return adminService.queryUserList(current, size);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/delete/{id}")
    public Result deleteUser(@PathVariable Long id) {
        return adminService.deleteUser(id);
    }

    // ==================== 订单管理 ====================

    /**
     * 查询商品订单列表
     */
    @GetMapping("/order/product/list")
    public Result listProductOrders(@RequestParam(defaultValue = "1") Integer current,
                                    @RequestParam(defaultValue = "10") Integer size) {
        return adminService.queryProductOrderList(current, size);
    }

    /**
     * 查询优惠券订单列表
     */
    @GetMapping("/order/voucher/list")
    public Result listVoucherOrders(@RequestParam(defaultValue = "1") Integer current,
                                    @RequestParam(defaultValue = "10") Integer size) {
        return adminService.queryVoucherOrderList(current, size);
    }

    /**
     * 商品订单发货
     */
    @PostMapping("/order/product/ship/{orderId}")
    public Result shipProductOrder(@PathVariable Long orderId) {
        return adminService.shipProductOrder(orderId);
    }

    // ==================== 预约管理 ====================

    /**
     * 查询所有预约列表
     */
    @GetMapping("/booking/list")
    public Result listBookings(@RequestParam(defaultValue = "1") Integer current,
                               @RequestParam(defaultValue = "10") Integer size) {
        return adminService.queryBookingList(current, size);
    }

    /**
     * 更新预约状态
     */
    @PostMapping("/booking/update-status/{id}")
    public Result updateBookingStatus(@PathVariable Long id,
                                      @RequestBody Map<String, Integer> params) {
        return adminService.updateBookingStatus(id, params.get("status"));
    }

    /**
     * 取消预约
     */
    @PostMapping("/booking/cancel/{id}")
    public Result cancelBooking(@PathVariable Long id) {
        return adminService.cancelBooking(id);
    }

    /**
     * 获取控制台统计数据
     */
    @GetMapping("/dashboard/stats")
    public Result getDashboardStats() {
        return adminService.getDashboardStats();
    }

    /**
     * 上传管理员头像
     */
    @PostMapping("/upload-icon")
    public Result uploadIcon(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("请选择图片");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + suffix;

            String uploadDir = System.getProperty("user.dir") + "/uploads/admin/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(uploadDir + filename);
            file.transferTo(dest);

            String url = "/uploads/admin/" + filename;
            return Result.ok(url);

        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.fail("头像上传失败");
        }
    }

    /**
     * 更新管理员头像
     */
    @PostMapping("/update-icon")
    public Result updateIcon(@RequestBody Map<String, String> params) {
        return adminService.updateIcon(params.get("icon"));
    }

    /**
     * 更新管理员姓名
     */
    @PostMapping("/update-name")
    public Result updateName(@RequestBody Map<String, String> params) {
        return adminService.updateName(params.get("name"));
    }
}
