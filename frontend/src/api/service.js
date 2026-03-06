import request from '../utils/request'

// 获取服务类型列表
export function getServiceTypeList(category) {
  return request({
    url: '/service/type/list',
    method: 'get',
    params: { category }
  })
}

// 获取服务类型详情
export function getServiceTypeDetail(id) {
  return request({
    url: `/service/type/detail/${id}`,
    method: 'get'
  })
}

// 创建预约
export function createBooking(data) {
  return request({
    url: '/service/booking',
    method: 'post',
    data
  })
}

// 获取我的预约列表
export function getMyBookings(status) {
  return request({
    url: '/service/booking/list',
    method: 'get',
    params: { status }
  })
}

// 获取预约详情
export function getBookingDetail(id) {
  return request({
    url: `/service/booking/detail/${id}`,
    method: 'get'
  })
}

// 取消预约
export function cancelBooking(id) {
  return request({
    url: `/service/booking/cancel/${id}`,
    method: 'post'
  })
}

// 提交服务评价
export function submitReview(bookingId, rating, content) {
  return request({
    url: `/service/review/${bookingId}`,
    method: 'post',
    params: { rating, content }
  })
}

// 获取服务评价列表
export function getReviewList(serviceTypeId, page, size) {
  return request({
    url: `/service/review/list/${serviceTypeId}`,
    method: 'get',
    params: { page, size }
  })
}

// 获取服务评分统计
export function getRatingStats(serviceTypeId) {
  return request({
    url: `/service/review/stats/${serviceTypeId}`,
    method: 'get'
  })
}
