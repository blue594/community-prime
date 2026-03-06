package com.community.prime.controller;

import com.community.prime.dto.ProductOrderDTO;
import com.community.prime.service.ProductService;
import com.community.prime.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商品控制器
 * 
 * 接口说明：
 * - GET  /product/list          : 查询商品列表
 * - GET  /product/detail/{id}   : 查询商品详情
 * - GET  /product/search        : 搜索商品
 * - POST /product/order         : 创建订单
 * - GET  /product/order/list    : 查询订单列表
 * - POST /product/order/cancel/{orderId} : 取消订单
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * 查询商品列表
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer current,
                       @RequestParam(defaultValue = "10") Integer size) {
        return productService.queryProductList(current, size);
    }

    /**
     * 根据ID查询商品详情
     */
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable("id") Long id) {
        return productService.queryProductById(id);
    }

    /**
     * 搜索商品
     */
    @GetMapping("/search")
    public Result search(@RequestParam("keyword") String keyword) {
        return productService.searchProduct(keyword);
    }

    /**
     * 创建订单
     */
    @PostMapping("/order")
    public Result createOrder(@RequestBody @Validated ProductOrderDTO productOrderDTO) {
        return productService.createOrder(productOrderDTO);
    }

    /**
     * 查询订单列表
     */
    @GetMapping("/order/list")
    public Result orderList(@RequestParam(required = false) Integer status,
                            @RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size) {
        return productService.queryOrderList(status, current, size);
    }

    /**
     * 取消订单
     */
    @PostMapping("/order/cancel/{orderId}")
    public Result cancelOrder(@PathVariable("orderId") Long orderId) {
        return productService.cancelOrder(orderId);
    }

    /**
     * 模拟支付订单
     */
    @PostMapping("/order/pay/{orderId}")
    public Result payOrder(@PathVariable("orderId") Long orderId) {
        return productService.payOrder(orderId);
    }

    /**
     * 查询秒杀商品列表
     */
    @GetMapping("/seckill/list")
    public Result seckillProductList() {
        return productService.querySeckillProductList();
    }

    /**
     * 秒杀商品下单
     */
    @PostMapping("/seckill/order/{seckillProductId}")
    public Result seckillProduct(@PathVariable("seckillProductId") Long seckillProductId,
                                  @RequestParam(required = false) String address,
                                  @RequestParam(required = false) String phone) {
        return productService.seckillProduct(seckillProductId, address, phone);
    }

    /**
     * 确认收货
     */
    @PostMapping("/order/confirm/{orderId}")
    public Result confirmReceipt(@PathVariable("orderId") Long orderId) {
        return productService.confirmReceipt(orderId);
    }
}
