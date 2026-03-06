package com.community.prime.config;

import com.community.prime.interceptor.AdminInterceptor;
import com.community.prime.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户登录拦截器 - 只拦截用户端路径
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/user/register",
                        "/user/upload-icon",
                        "/product/list",
                        "/product/detail/**",
                        "/product/seckill/list",
                        "/voucher/list/**",
                        "/voucher/normal/**",
                        "/voucher/seckill/detail/**",
                        "/service-type/**",
                        "/service/type/**",
                        "/service/review/list/**",
                        "/service/review/stats/**",
                        "/admin/**",
                        "/uploads/**",
                        "/error"
                );

        // 管理员登录拦截器 - 只拦截管理员路径，排除登录接口
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/login",
                        "/admin/logout"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件访问路径
        String uploadPath = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
