package com.community.prime.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.prime.dto.ServiceBookingDTO;
import com.community.prime.entity.ServiceBooking;
import com.community.prime.vo.Result;

/**
 * 服务预约服务接口
 */
public interface ServiceBookingService extends IService<ServiceBooking> {

    /**
     * 查询服务类型列表
     * 
     * @param category 分类 1-家政 2-维修
     * @return 服务类型列表
     */
    Result queryServiceTypeList(Integer category);

    /**
     * 查询服务类型详情
     * 
     * @param id 服务类型ID
     * @return 服务详情
     */
    Result queryServiceTypeDetail(Long id);

    /**
     * 创建服务预约
     * 
     * @param bookingDTO 预约信息
     * @return 预约结果
     */
    Result createBooking(ServiceBookingDTO bookingDTO);

    /**
     * 查询我的预约列表
     * 
     * @param status 预约状态
     * @return 预约列表
     */
    Result queryMyBookings(Integer status);

    /**
     * 取消预约
     * 
     * @param bookingId 预约ID
     * @return 取消结果
     */
    Result cancelBooking(Long bookingId);

    /**
     * 查询预约详情
     * 
     * @param bookingId 预约ID
     * @return 预约详情
     */
    Result queryBookingDetail(Long bookingId);

    /**
     * 提交服务评价
     * 
     * @param bookingId 预约ID
     * @param rating    评分（1-5）
     * @param content   评价内容
     * @return 提交结果
     */
    Result submitReview(Long bookingId, Integer rating, String content);

    /**
     * 查询服务类型的评价列表
     * 
     * @param serviceTypeId 服务类型ID
     * @param page          页码
     * @param size          每页数量
     * @return 评价列表
     */
    Result queryReviews(Long serviceTypeId, Integer page, Integer size);

    /**
     * 查询服务类型的评分统计
     * 
     * @param serviceTypeId 服务类型ID
     * @return 评分统计
     */
    Result queryRatingStats(Long serviceTypeId);
}
