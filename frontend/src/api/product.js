import request from '../utils/request'

// 获取商品列表
export function getProductList(params) {
  return request({
    url: '/product/list',
    method: 'get',
    params
  })
}

// 获取商品详情
export function getProductDetail(id) {
  return request({
    url: `/product/detail/${id}`,
    method: 'get'
  })
}

// 搜索商品
export function searchProduct(keyword) {
  return request({
    url: '/product/search',
    method: 'get',
    params: { keyword }
  })
}

// 创建订单
export function createOrder(data) {
  return request({
    url: '/product/order',
    method: 'post',
    data
  })
}

// 获取订单列表
export function getOrderList(params) {
  return request({
    url: '/product/order/list',
    method: 'get',
    params
  })
}

// 取消订单
export function cancelOrder(orderId) {
  return request({
    url: `/product/order/cancel/${orderId}`,
    method: 'post'
  })
}

// 支付订单
export function payOrder(orderId) {
  return request({
    url: `/product/order/pay/${orderId}`,
    method: 'post'
  })
}

// 获取秒杀商品列表
export function getSeckillProductList() {
  return request({
    url: '/product/seckill/list',
    method: 'get'
  })
}

// 秒杀商品下单
export function seckillProductOrder(seckillProductId, address, phone) {
  return request({
    url: `/product/seckill/order/${seckillProductId}`,
    method: 'post',
    params: { address, phone }
  })
}

// 确认收货
export function confirmReceipt(orderId) {
  return request({
    url: `/product/order/confirm/${orderId}`,
    method: 'post'
  })
}
