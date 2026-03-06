package com.community.prime.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson配置类
 *
 * 解决前端Long类型精度丢失问题
 * JavaScript的Number类型最大安全整数为2^53-1（约9007199254740991）
 * 超过此范围的Long类型会丢失精度，需要将Long转为String返回
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置Jackson将Long类型序列化为String
     * 解决前端JavaScript精度丢失问题
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 将Long类型序列化为String，解决前端精度丢失问题
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
