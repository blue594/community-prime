package com.community.prime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.prime.entity.ServiceBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 服务预约Mapper接口
 * 
 * 索引设计说明：
 * 1. idx_user_id(user_id) - 查询用户预约
 * 2. idx_booking_date(booking_date) - 查询某日预约
 * 3. idx_status(status) - 按状态查询
 * 4. idx_service_type(service_type_id) - 按服务类型查询
 */
@Mapper
public interface ServiceBookingMapper extends BaseMapper<ServiceBooking> {

    /**
     * 查询某时间段是否已被预约
     */
    int countConflictBooking(@Param("serviceTypeId") Long serviceTypeId,
                              @Param("bookingDate") LocalDate bookingDate,
                              @Param("startTime") LocalTime startTime,
                              @Param("endTime") LocalTime endTime);

    /**
     * 查询用户的预约列表
     */
    List<ServiceBooking> selectByUserId(@Param("userId") Long userId, 
                                        @Param("status") Integer status);
}
