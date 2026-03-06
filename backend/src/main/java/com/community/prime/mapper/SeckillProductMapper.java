package com.community.prime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.prime.entity.SeckillProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品秒杀Mapper接口
 */
@Mapper
public interface SeckillProductMapper extends BaseMapper<SeckillProduct> {

    /**
     * 查询秒杀商品列表（关联查询商品信息）
     */
    @Select("SELECT sp.*, p.name AS product_name, p.image AS product_image, p.price AS original_price " +
            "FROM tb_seckill_product sp " +
            "LEFT JOIN tb_product p ON sp.product_id = p.id " +
            "WHERE sp.deleted = 0 AND p.status = 1 " +
            "ORDER BY sp.begin_time ASC")
    List<SeckillProduct> selectSeckillProductList();

    /**
     * 查询秒杀商品详情（关联查询商品信息）
     */
    @Select("SELECT sp.*, p.name AS product_name, p.image AS product_image, p.price AS original_price " +
            "FROM tb_seckill_product sp " +
            "LEFT JOIN tb_product p ON sp.product_id = p.id " +
            "WHERE sp.id = #{id} AND sp.deleted = 0")
    SeckillProduct selectSeckillProductById(@Param("id") Long id);

    /**
     * 扣减秒杀商品库存（乐观锁）
     */
    @Update("UPDATE tb_seckill_product SET stock = stock - 1 " +
            "WHERE id = #{id} AND stock > 0")
    int deductStock(@Param("id") Long id);
}
