package com.community.prime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.prime.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 优惠券Mapper接口
 * 
 * 索引设计说明：
 * 1. idx_type_status(type, status) - 查询优惠券列表
 * 2. idx_shop_id(shop_id) - 查询商家优惠券
 * 3. idx_time(begin_time, end_time) - 查询有效期内优惠券
 */
@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    /**
     * 查询商家优惠券列表（使用索引优化）
     * 
     * 使用索引：idx_shop_id
     * 
     * @param shopId 商家ID
     * @return 优惠券列表
     */
    @Select("SELECT id, shop_id, title, sub_title, rules, pay_value, actual_value, " +
            "type, stock, begin_time, end_time " +
            "FROM tb_voucher " +
            "WHERE shop_id = #{shopId} AND deleted = 0 " +
            "ORDER BY create_time DESC")
    List<Voucher> selectListByShopId(@Param("shopId") Long shopId);

    /**
     * 扣减秒杀券库存（乐观锁）
     * 
     * 使用索引：主键索引
     * 
     * @param voucherId 优惠券ID
     * @return 影响行数
     */
    @Update("UPDATE tb_voucher SET stock = stock - 1 " +
            "WHERE id = #{voucherId} AND stock > 0")
    int deductStock(@Param("voucherId") Long voucherId);

    /**
     * 查询普通优惠券列表（非秒杀券）
     *
     * @param shopId 商家ID
     * @return 优惠券列表
     */
    @Select("SELECT id, shop_id, title, sub_title, rules, pay_value, actual_value, " +
            "type, stock, begin_time, end_time " +
            "FROM tb_voucher " +
            "WHERE shop_id = #{shopId} AND type = 0 AND deleted = 0 " +
            "ORDER BY create_time DESC")
    List<Voucher> selectNormalVouchersByShopId(@Param("shopId") Long shopId);

    /**
     * 查询所有优惠券列表（管理员用，不限制商家）
     *
     * @return 优惠券列表
     */
    @Select("SELECT id, shop_id, title, sub_title, rules, pay_value, actual_value, " +
            "type, stock, begin_time, end_time, create_time " +
            "FROM tb_voucher " +
            "WHERE deleted = 0 " +
            "ORDER BY create_time DESC")
    List<Voucher> selectAllList();
}
