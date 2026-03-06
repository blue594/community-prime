package com.community.prime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.community.prime.entity.ProductOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品订单Mapper接口
 */
@Mapper
public interface ProductOrderMapper extends BaseMapper<ProductOrder> {

    /**
     * 优化后的订单分页查询（使用覆盖索引）
     */
    IPage<ProductOrder> selectOrderPage(@Param("page") IPage<ProductOrder> page,
                                        @Param("userId") Long userId,
                                        @Param("status") Integer status);

    /**
     * 性能问题版本（仅用于演示对比，实际不要使用）
     */
    @Deprecated
    IPage<ProductOrder> selectOrderPageBad(@Param("page") IPage<ProductOrder> page,
                                           @Param("userId") Long userId,
                                           @Param("status") Integer status);
}