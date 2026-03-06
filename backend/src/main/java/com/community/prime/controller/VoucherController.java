package com.community.prime.controller;

import com.community.prime.entity.Voucher;
import com.community.prime.service.VoucherService;
import com.community.prime.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 优惠券控制器
 * 
 * 接口说明：
 * - POST /voucher/seckill/add           : 添加秒杀券（管理员）
 * - GET  /voucher/list/{shopId}         : 查询商家优惠券
 * - GET  /voucher/normal/{shopId}       : 查询商家普通优惠券（到店券）
 * - GET  /voucher/seckill/detail/{id}   : 查询秒杀券详情
 * - POST /voucher/seckill/order/{id}    : 秒杀下单
 * - POST /voucher/buy/{id}             : 购买普通优惠券
 * - GET  /voucher/order/list            : 查询我的优惠券订单
 * - POST /voucher/order/pay/{orderId}   : 支付优惠券订单
 * - POST /voucher/order/verify/{orderId}: 核销优惠券
 */
@Slf4j
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private VoucherService voucherService;

    /**
     * 添加秒杀券
     */
    @PostMapping("/seckill/add")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        return voucherService.addSeckillVoucher(voucher);
    }

    /**
     * 查询商家的优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }

    /**
     * 查询商家的普通优惠券列表（到店券、餐饮券等）
     */
    @GetMapping("/normal/{shopId}")
    public Result queryNormalVouchers(@PathVariable("shopId") Long shopId) {
        return voucherService.queryNormalVouchers(shopId);
    }

    /**
     * 购买普通优惠券（非秒杀，直接下单）
     */
    @PostMapping("/buy/{id}")
    public Result buyVoucher(@PathVariable("id") Long voucherId) {
        return voucherService.buyVoucher(voucherId);
    }

    /**
     * 查询秒杀券详情
     */
    @GetMapping("/seckill/detail/{id}")
    public Result querySeckillVoucher(@PathVariable("id") Long id) {
        return voucherService.querySeckillVoucher(id);
    }

    /**
     * 秒杀下单
     * 
     * 技术说明：
     * 1. 使用Redisson分布式锁保证一人一单
     * 2. 使用Redis Stream实现异步订单创建
     * 3. 使用Lua脚本预减库存
     */
    @PostMapping("/seckill/order/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherService.seckillVoucher(voucherId);
    }

    /**
     * 查询我的优惠券订单
     */
    @GetMapping("/order/list")
    public Result queryMyVoucherOrders() {
        return voucherService.queryMyVoucherOrders();
    }

    /**
     * 支付优惠券订单（模拟支付）
     */
    @PostMapping("/order/pay/{orderId}")
    public Result payVoucherOrder(@PathVariable("orderId") Long orderId) {
        return voucherService.payVoucherOrder(orderId);
    }

    /**
     * 核销优惠券
     */
    @PostMapping("/order/verify/{orderId}")
    public Result verifyVoucher(@PathVariable("orderId") Long orderId) {
        return voucherService.verifyVoucher(orderId);
    }
}
