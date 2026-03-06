package com.community.prime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.prime.entity.VoucherOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 优惠券订单Mapper接口
 * 
 * 索引设计说明：
 * 1. idx_user_id(user_id) - 查询用户订单
 * 2. idx_voucher_id(voucher_id) - 查询优惠券订单
 * 3. idx_user_voucher(user_id, voucher_id) - 查询用户是否购买过某优惠券（唯一性校验）
 * 4. idx_status(status) - 按状态查询订单
 */
@Mapper
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    /**
     * 查询用户是否已购买该优惠券
     * 
     * 使用索引：idx_user_voucher(user_id, voucher_id)
     * 
     * @param userId    用户ID
     * @param voucherId 优惠券ID
     * @return 订单数量
     */
    @Select("SELECT COUNT(*) FROM tb_voucher_order " +
            "WHERE user_id = #{userId} AND voucher_id = #{voucherId}")
    int countByUserIdAndVoucherId(@Param("userId") Long userId, 
                                   @Param("voucherId") Long voucherId);

    /**
     * 查询用户购买某优惠券的订单（用于幂等性校验）
     * 
     * 使用索引：idx_user_voucher(user_id, voucher_id)
     * 
     * @param userId    用户ID
     * @param voucherId 优惠券ID
     * @return 订单ID
     */
    @Select("SELECT id FROM tb_voucher_order " +
            "WHERE user_id = #{userId} AND voucher_id = #{voucherId} LIMIT 1")
    Long selectOrderIdByUserAndVoucher(@Param("userId") Long userId, 
                                        @Param("voucherId") Long voucherId);
}
