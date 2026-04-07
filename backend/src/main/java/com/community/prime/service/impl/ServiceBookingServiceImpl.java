package com.community.prime.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.prime.dto.BookingMessageDTO;
import com.community.prime.dto.ServiceBookingDTO;
import com.community.prime.entity.ServiceBooking;
import com.community.prime.entity.ServiceReview;
import com.community.prime.entity.ServiceType;
import com.community.prime.handler.BusinessException;
import com.community.prime.mapper.ServiceBookingMapper;
import com.community.prime.mapper.ServiceReviewMapper;
import com.community.prime.mapper.ServiceTypeMapper;
import com.community.prime.mq.BookingMessageProducer;
import com.community.prime.service.ServiceBookingService;
import com.community.prime.utils.UserHolder;
import com.community.prime.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务预约服务实现类
 *
 * 【面试重点】RabbitMQ 集成说明：
 * 1. 核心流程（createBooking）：数据库事务提交后发送 MQ 消息
 * 2. 解耦原则：预约创建与通知逻辑分离，提升接口响应速度
 * 3. 可靠性：消息发送失败不影响主流程，记录日志后续补偿
 * 4. 异步削峰：MQ 缓冲通知请求，保护下游短信/推送服务
 */
@Slf4j
@Service
public class ServiceBookingServiceImpl extends ServiceImpl<ServiceBookingMapper, ServiceBooking>
        implements ServiceBookingService {

    @Resource
    private ServiceTypeMapper serviceTypeMapper;

    @Resource
    private ServiceReviewMapper serviceReviewMapper;

    /**
     * 注入 RabbitMQ 消息生产者
     * 用于发送预约创建通知消息
     */
    @Resource
    private BookingMessageProducer bookingMessageProducer;

    @Override
    public Result queryServiceTypeList(Integer category) {
        List<ServiceType> list = serviceTypeMapper.selectActiveList(category);
        return Result.ok(list);
    }

    @Override
    public Result queryServiceTypeDetail(Long id) {
        ServiceType serviceType = serviceTypeMapper.selectById(id);
        if (serviceType == null || serviceType.getDeleted() == 1) {
            return Result.fail("服务类型不存在");
        }
        return Result.ok(serviceType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createBooking(ServiceBookingDTO bookingDTO) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询服务类型
        ServiceType serviceType = serviceTypeMapper.selectById(bookingDTO.getServiceTypeId());
        if (serviceType == null || serviceType.getStatus() != 1) {
            throw new BusinessException("服务类型不存在或已下架");
        }

        // 2. 校验预约日期（不能预约过去的时间）
        LocalDate bookingDate = bookingDTO.getBookingDate();
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new BusinessException("不能预约过去的时间");
        }

        // 3. 计算结束时间（默认服务时长2小时）
        LocalTime startTime = bookingDTO.getStartTime();
        LocalTime endTime = startTime.plusHours(2);

        // 4. 检查时间段是否冲突
        int conflictCount = baseMapper.countConflictBooking(
                bookingDTO.getServiceTypeId(),
                bookingDate,
                startTime,
                endTime
        );
        if (conflictCount > 0) {
            throw new BusinessException("该时间段已被预约，请选择其他时间");
        }

        // 5. 创建预约
        ServiceBooking booking = new ServiceBooking();
        BeanUtil.copyProperties(bookingDTO, booking);
        booking.setUserId(userId);
        booking.setServiceName(serviceType.getName());
        booking.setPrice(serviceType.getBasePrice());
        booking.setEndTime(endTime);
        booking.setStatus(0); // 待确认

        save(booking);

        log.info("用户{}创建预约成功，预约ID：{}，服务：{}",
                userId, booking.getId(), serviceType.getName());

        // ==================== RabbitMQ 发送异步通知消息 ====================
        // 【面试重点】为什么放在事务提交后？
        // 1. 保证数据库操作成功后才发送消息，避免消息已发但事务回滚
        // 2. 消息发送与数据库操作不在同一事务，发送失败不影响主流程
        // 3. 异步解耦：接口立即返回，通知逻辑异步处理，提升响应速度
        // 4. 削峰填谷：MQ 缓冲请求，保护下游短信/推送服务

        try {
            // 构造消息 DTO
            BookingMessageDTO messageDTO = BookingMessageDTO.createBookingMessage(
                    booking.getId(),
                    userId,
                    serviceType.getName(),
                    booking.getBookingDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    booking.getStartTime().toString()
            );

            // 发送消息到 RabbitMQ
            boolean sendSuccess = bookingMessageProducer.sendBookingCreateMessage(messageDTO);

            if (sendSuccess) {
                log.info("[createBooking] 预约通知消息发送成功, bookingId: {}", booking.getId());
            } else {
                // 发送失败记录日志，不阻塞主流程，后续可通过补偿机制处理
                log.warn("[createBooking] 预约通知消息发送失败, bookingId: {}, 后续需补偿处理", booking.getId());
            }

        } catch (Exception e) {
            // 【面试点】异常处理：记录日志但不抛出，不影响主流程
            log.error("[createBooking] 发送 MQ 消息异常, bookingId: {}, error: {}",
                    booking.getId(), e.getMessage(), e);
        }

        return Result.ok(booking.getId());
    }

    @Override
    public Result queryMyBookings(Integer status) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        List<ServiceBooking> bookings = baseMapper.selectByUserId(userId, status);

        // 填充 reviewed 字段（已完成的预约才检查是否已评价）
        for (ServiceBooking booking : bookings) {
            if (booking.getStatus() != null && booking.getStatus() == 3) {
                int reviewCount = serviceReviewMapper.countByBookingId(booking.getId());
                booking.setReviewed(reviewCount > 0);
            } else {
                booking.setReviewed(false);
            }
        }

        return Result.ok(bookings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelBooking(Long bookingId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询预约
        ServiceBooking booking = getById(bookingId);
        if (booking == null) {
            throw new BusinessException("预约不存在");
        }

        // 2. 校验归属
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该预约");
        }

        // 3. 校验状态（只能取消待确认或已确认的预约）
        if (booking.getStatus() != 0 && booking.getStatus() != 1) {
            throw new BusinessException("当前状态不允许取消");
        }

        // 4. 校验时间（服务开始前2小时可以取消）
        LocalDateTime serviceTime = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        if (serviceTime.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BusinessException("服务开始前2小时内不能取消");
        }

        // 5. 更新状态
        booking.setStatus(4); // 已取消
        booking.setUpdateTime(LocalDateTime.now());
        updateById(booking);

        log.info("用户{}取消预约成功，预约ID：{}", userId, bookingId);
        return Result.ok();
    }

    @Override
    public Result queryBookingDetail(Long bookingId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        ServiceBooking booking = getById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException("预约不存在");
        }

        return Result.ok(booking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result submitReview(Long bookingId, Integer rating, String content) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 查询预约
        ServiceBooking booking = getById(bookingId);
        if (booking == null) {
            throw new BusinessException("预约不存在");
        }

        // 2. 校验归属
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("无权评价该预约");
        }

        // 3. 校验状态（只有已完成的预约可以评价）
        if (booking.getStatus() != 3) {
            throw new BusinessException("只有已完成的服务才能评价");
        }

        // 4. 校验是否已评价
        int existCount = serviceReviewMapper.countByBookingId(bookingId);
        if (existCount > 0) {
            throw new BusinessException("该服务已评价，不可重复评价");
        }

        // 5. 校验评分
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }

        // 6. 创建评价
        ServiceReview review = new ServiceReview();
        review.setUserId(userId);
        review.setBookingId(bookingId);
        review.setServiceTypeId(booking.getServiceTypeId());
        review.setRating(rating);
        review.setContent(content);
        serviceReviewMapper.insert(review);

        log.info("用户{}评价预约{}成功，评分：{}", userId, bookingId, rating);
        return Result.ok("评价成功");
    }

    @Override
    public Result queryReviews(Long serviceTypeId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        int offset = (page - 1) * size;
        List<ServiceReview> reviews = serviceReviewMapper.selectByServiceTypeId(serviceTypeId, offset, size);
        int total = serviceReviewMapper.selectReviewCount(serviceTypeId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", reviews);
        result.put("total", total);
        return Result.ok(result);
    }

    @Override
    public Result queryRatingStats(Long serviceTypeId) {
        Double avgRating = serviceReviewMapper.selectAvgRating(serviceTypeId);
        int reviewCount = serviceReviewMapper.selectReviewCount(serviceTypeId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("avgRating", Math.round(avgRating * 10) / 10.0);
        stats.put("reviewCount", reviewCount);
        return Result.ok(stats);
    }
}
