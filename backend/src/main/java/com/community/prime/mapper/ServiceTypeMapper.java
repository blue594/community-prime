package com.community.prime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.prime.entity.ServiceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务类型Mapper接口
 */
@Mapper
public interface ServiceTypeMapper extends BaseMapper<ServiceType> {

    /**
     * 查询上架的服务类型列表（用户端用）
     * 
     * @param category 分类 1-家政 2-维修
     * @return 服务类型列表
     */
    List<ServiceType> selectActiveList(@Param("category") Integer category);

    /**
     * 查询所有服务类型列表（管理员端用）
     * 
     * @return 服务类型列表
     */
    List<ServiceType> selectAllList();
}
