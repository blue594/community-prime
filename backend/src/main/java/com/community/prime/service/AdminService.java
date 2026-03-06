package com.community.prime.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.prime.entity.Admin;
import com.community.prime.vo.Result;

import javax.servlet.http.HttpSession;

/**
 * 管理员服务接口
 */
public interface AdminService extends IService<Admin> {

    /**
     * 管理员登录
     *
     * @param username 账号
     * @param password 密码
     * @param session  session
     * @return 登录结果
     */
    Result login(String username, String password, HttpSession session);

    /**
     * 获取当前登录管理员
     *
     * @return 管理员信息
     */
    Result getCurrentAdmin();

    /**
     * 退出登录
     */
    Result logout();

    /**
     * 修改密码
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 结果
     */
    Result changePassword(String oldPassword, String newPassword);

    /**
     * 创建管理员
     *
     * @param admin 管理员信息
     * @return 结果
     */
    Result createAdmin(Admin admin);

    /**
     * 更新管理员
     *
     * @param admin 管理员信息
     * @return 结果
     */
    Result updateAdmin(Admin admin);

    /**
     * 删除管理员
     *
     * @param id 管理员ID
     * @return 结果
     */
    Result deleteAdmin(Long id);

    /**
     * 查询用户列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 用户列表
     */
    Result queryUserList(Integer current, Integer size);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 结果
     */
    Result deleteUser(Long id);

    /**
     * 查询商品订单列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 订单列表
     */
    Result queryProductOrderList(Integer current, Integer size);

    /**
     * 查询优惠券订单列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 订单列表
     */
    Result queryVoucherOrderList(Integer current, Integer size);

    /**
     * 商品订单发货
     *
     * @param orderId 订单ID
     * @return 结果
     */
    Result shipProductOrder(Long orderId);

    /**
     * 查询预约列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 预约列表
     */
    Result queryBookingList(Integer current, Integer size);

    /**
     * 更新预约状态
     *
     * @param id     预约ID
     * @param status 状态
     * @return 结果
     */
    Result updateBookingStatus(Long id, Integer status);

    /**
     * 取消预约
     *
     * @param id 预约ID
     * @return 结果
     */
    Result cancelBooking(Long id);

    /**
     * 获取统计数据（商品数、订单数、用户数、预约数）
     *
     * @return 统计数据
     */
    Result getDashboardStats();

    /**
     * 更新管理员头像
     *
     * @param icon 头像URL
     * @return 更新结果
     */
    Result updateIcon(String icon);

    /**
     * 更新管理员姓名
     *
     * @param name 姓名
     * @return 更新结果
     */
    Result updateName(String name);
}
