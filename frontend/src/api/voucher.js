import request from '../utils/request'

// 获取商家的优惠券列表
export function getVoucherList(shopId) {
  return request({
    url: `/voucher/list/${shopId}`,
    method: 'get'
  })
}

// 获取商家的普通优惠券列表（到店券）
export function getNormalVouchers(shopId) {
  return request({
    url: `/voucher/normal/${shopId}`,
    method: 'get'
  })
}

// 购买普通优惠券
export function buyVoucher(voucherId) {
  return request({
    url: `/voucher/buy/${voucherId}`,
    method: 'post'
  })
}

// 获取秒杀券详情
export function getSeckillVoucher(id) {
  return request({
    url: `/voucher/seckill/detail/${id}`,
    method: 'get'
  })
}

// 秒杀下单
export function seckillOrder(voucherId) {
  return request({
    url: `/voucher/seckill/order/${voucherId}`,
    method: 'post'
  })
}

// 获取我的优惠券订单
export function getMyVoucherOrders() {
  return request({
    url: '/voucher/order/list',
    method: 'get'
  })
}

// 支付优惠券订单
export function payVoucherOrder(orderId) {
  return request({
    url: `/voucher/order/pay/${orderId}`,
    method: 'post'
  })
}

// 核销优惠券
export function verifyVoucher(orderId) {
  return request({
    url: `/voucher/order/verify/${orderId}`,
    method: 'post'
  })
}
