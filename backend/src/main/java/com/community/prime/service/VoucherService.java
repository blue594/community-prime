package com.community.prime.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.prime.entity.Voucher;
import com.community.prime.vo.Result;

/**
 * 优惠券服务接口
 */
public interface VoucherService extends IService<Voucher> {

    /**
     * 添加秒杀券
     * 
     * @param voucher 优惠券信息
     * @return 添加结果
     */
    Result addSeckillVoucher(Voucher voucher);

    /**
     * 查询商家优惠券列表
     * 
     * @param shopId 商家ID
     * @return 优惠券列表
     */
    Result queryVoucherOfShop(Long shopId);

    /**
     * 查询秒杀券详情
     * 
     * @param voucherId 优惠券ID
     * @return 优惠券详情
     */
    Result querySeckillVoucher(Long voucherId);

    /**
     * 秒杀下单
     * 
     * 技术说明：
     * 1. 使用Redisson分布式锁保证原子性
     * 2. 使用Redis Stream实现异步订单创建
     * 3. 一人一单限制
     * 4. 库存预热到Redis
     * 
     * @param voucherId 优惠券ID
     * @return 下单结果
     */
    // todo 分布式锁
    Result seckillVoucher(Long voucherId);

    /**
     * 创建秒杀订单（供消息队列消费）
     * 
     * @param voucherId 优惠券ID
     * @param userId    用户ID
     * @param orderId   订单ID
     */
    void createVoucherOrder(Long voucherId, Long userId, Long orderId);

    /**
     * 查询用户秒杀订单
     * 
     * @return 订单列表
     */
    Result queryMyVoucherOrders();

    /**
     * 支付优惠券订单（模拟支付）
     * 
     * @param orderId 订单ID
     * @return 支付结果
     */
    Result payVoucherOrder(Long orderId);

    /**
     * 核销优惠券
     * 
     * @param orderId 订单ID
     * @return 核销结果
     */
    Result verifyVoucher(Long orderId);

    /**
     * 购买普通优惠券（非秒杀，直接下单）
     * 
     * @param voucherId 优惠券ID
     * @return 下单结果
     */
    Result buyVoucher(Long voucherId);

    /**
     * 查询商家普通优惠券列表（到店券）
     * 
     * @param shopId 商家ID
     * @return 优惠券列表
     */
    Result queryNormalVouchers(Long shopId);
}
