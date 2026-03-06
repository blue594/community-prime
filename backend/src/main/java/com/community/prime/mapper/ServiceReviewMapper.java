package com.community.prime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.prime.entity.ServiceReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务评价Mapper接口
 * 
 * 索引设计说明：
 * 1. idx_booking_id(booking_id) - 查询某预约的评价
 * 2. idx_service_type_id(service_type_id) - 查询某服务的所有评价
 * 3. idx_user_id(user_id) - 查询某用户的所有评价
 */
@Mapper
public interface ServiceReviewMapper extends BaseMapper<ServiceReview> {

    /**
     * 查询某服务类型的平均评分
     */
    @Select("SELECT IFNULL(AVG(rating), 0) FROM tb_service_review " +
            "WHERE service_type_id = #{serviceTypeId} AND deleted = 0")
    Double selectAvgRating(@Param("serviceTypeId") Long serviceTypeId);

    /**
     * 查询某服务类型的评价数量
     */
    @Select("SELECT COUNT(*) FROM tb_service_review " +
            "WHERE service_type_id = #{serviceTypeId} AND deleted = 0")
    int selectReviewCount(@Param("serviceTypeId") Long serviceTypeId);

    /**
     * 查询某预约是否已评价
     */
    @Select("SELECT COUNT(*) FROM tb_service_review " +
            "WHERE booking_id = #{bookingId} AND deleted = 0")
    int countByBookingId(@Param("bookingId") Long bookingId);

    /**
     * 查询某服务类型的评价列表
     */
    @Select("SELECT r.*, u.nick_name as user_nick_name FROM tb_service_review r " +
            "LEFT JOIN tb_user u ON r.user_id = u.id " +
            "WHERE r.service_type_id = #{serviceTypeId} AND r.deleted = 0 " +
            "ORDER BY r.create_time DESC LIMIT #{offset}, #{limit}")
    List<ServiceReview> selectByServiceTypeId(@Param("serviceTypeId") Long serviceTypeId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);
}
