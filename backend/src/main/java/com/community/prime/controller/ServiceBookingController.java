package com.community.prime.controller;

import com.community.prime.dto.ServiceBookingDTO;
import com.community.prime.service.ServiceBookingService;
import com.community.prime.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 服务预约控制器
 * 
 * 接口说明：
 * - GET  /service/type/list       : 查询服务类型列表
 * - GET  /service/type/detail/{id}: 查询服务类型详情
 * - POST /service/booking         : 创建预约
 * - GET  /service/booking/list    : 查询我的预约列表
 * - GET  /service/booking/detail/{id} : 查询预约详情
 * - POST /service/booking/cancel/{id} : 取消预约
 * - POST /service/review/{bookingId}  : 提交服务评价
 * - GET  /service/review/list/{serviceTypeId} : 查询服务评价列表
 * - GET  /service/review/stats/{serviceTypeId}: 查询服务评分统计
 */
@Slf4j
@RestController
@RequestMapping("/service")
public class ServiceBookingController {

    @Resource
    private ServiceBookingService serviceBookingService;

    /**
     * 查询服务类型列表
     */
    @GetMapping("/type/list")
    public Result typeList(@RequestParam(required = false) Integer category) {
        return serviceBookingService.queryServiceTypeList(category);
    }

    /**
     * 查询服务类型详情
     */
    @GetMapping("/type/detail/{id}")
    public Result typeDetail(@PathVariable("id") Long id) {
        return serviceBookingService.queryServiceTypeDetail(id);
    }

    /**
     * 创建服务预约
     */
    @PostMapping("/booking")
    public Result createBooking(@RequestBody @Validated ServiceBookingDTO bookingDTO) {
        return serviceBookingService.createBooking(bookingDTO);
    }

    /**
     * 查询我的预约列表
     */
    @GetMapping("/booking/list")
    public Result bookingList(@RequestParam(required = false) Integer status) {
        return serviceBookingService.queryMyBookings(status);
    }

    /**
     * 查询预约详情
     */
    @GetMapping("/booking/detail/{id}")
    public Result bookingDetail(@PathVariable("id") Long id) {
        return serviceBookingService.queryBookingDetail(id);
    }

    /**
     * 取消预约
     */
    @PostMapping("/booking/cancel/{id}")
    public Result cancelBooking(@PathVariable("id") Long id) {
        return serviceBookingService.cancelBooking(id);
    }

    /**
     * 提交服务评价
     */
    @PostMapping("/review/{bookingId}")
    public Result submitReview(@PathVariable("bookingId") Long bookingId,
                                @RequestParam Integer rating,
                                @RequestParam(required = false) String content) {
        return serviceBookingService.submitReview(bookingId, rating, content);
    }

    /**
     * 查询服务评价列表
     */
    @GetMapping("/review/list/{serviceTypeId}")
    public Result reviewList(@PathVariable("serviceTypeId") Long serviceTypeId,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size) {
        return serviceBookingService.queryReviews(serviceTypeId, page, size);
    }

    /**
     * 查询服务评分统计
     */
    @GetMapping("/review/stats/{serviceTypeId}")
    public Result ratingStats(@PathVariable("serviceTypeId") Long serviceTypeId) {
        return serviceBookingService.queryRatingStats(serviceTypeId);
    }
}
