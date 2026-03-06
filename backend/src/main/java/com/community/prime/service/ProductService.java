package com.community.prime.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.prime.dto.ProductOrderDTO;
import com.community.prime.entity.Product;
import com.community.prime.vo.PageResult;
import com.community.prime.vo.ProductVO;
import com.community.prime.vo.Result;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService extends IService<Product> {

    /**
     * 查询商品列表（分页）- 只返回上架商品
     * 
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    Result queryProductList(Integer current, Integer size);

    /**
     * 查询所有商品列表（分页）- 管理员用，返回所有状态商品
     * 
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    Result queryAllProductList(Integer current, Integer size);

    /**
     * 根据ID查询商品详情（带缓存）
     * 
     * 技术说明：
     * - 使用缓存穿透防护：缓存空值
     * - 使用缓存击穿防护：互斥锁
     * - 使用缓存雪崩防护：随机过期时间
     * 
     * @param id 商品ID
     * @return 商品详情
     */
    Result queryProductById(Long id);

    /**
     * 搜索商品
     * 
     * @param keyword 关键词
     * @return 商品列表
     */
    Result searchProduct(String keyword);

    /**
     * 创建商品订单
     * 
     * @param productOrderDTO 订单信息
     * @return 订单结果
     */
    Result createOrder(ProductOrderDTO productOrderDTO);

    /**
     * 查询用户订单列表
     * 
     * @param status 订单状态
     * @param current 当前页
     * @param size 每页大小
     * @return 订单列表
     */
    Result queryOrderList(Integer status, Integer current, Integer size);

    /**
     * 取消订单
     * 
     * @param orderId 订单ID
     * @return 取消结果
     */
    Result cancelOrder(Long orderId);

    /**
     * 支付订单（模拟支付）
     * 
     * @param orderId 订单ID
     * @return 支付结果
     */
    Result payOrder(Long orderId);

    /**
     * 查询秒杀商品列表
     * 
     * @return 秒杀商品列表
     */
    Result querySeckillProductList();

    /**
     * 秒杀商品下单
     *
     * @param seckillProductId 秒杀商品ID
     * @param address 收货地址
     * @param phone 联系电话
     * @return 下单结果
     */
    Result seckillProduct(Long seckillProductId, String address, String phone);

    /**
     * 确认收货
     *
     * @param orderId 订单ID
     * @return 确认结果
     */
    Result confirmReceipt(Long orderId);
}
