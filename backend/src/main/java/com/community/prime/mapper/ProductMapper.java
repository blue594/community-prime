package com.community.prime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.prime.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品Mapper接口
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 扣减库存
     * 
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 影响行数
     */
    @Update("UPDATE tb_product SET stock = stock - #{quantity}, sold = sold + #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 搜索商品（使用索引优化）
     * 
     * @param keyword 关键词
     * @return 商品列表
     */
    @Select("SELECT id, name, image, price, stock, sold FROM tb_product " +
            "WHERE status = 1 AND name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY sold DESC LIMIT 20")
    List<Product> searchByKeyword(@Param("keyword") String keyword);
}
