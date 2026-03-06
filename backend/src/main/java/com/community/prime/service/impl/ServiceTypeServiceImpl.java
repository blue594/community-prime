package com.community.prime.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.prime.entity.ServiceType;
import com.community.prime.mapper.ServiceTypeMapper;
import com.community.prime.service.ServiceTypeService;
import org.springframework.stereotype.Service;

@Service
public class ServiceTypeServiceImpl extends ServiceImpl<ServiceTypeMapper, ServiceType> implements ServiceTypeService {
}