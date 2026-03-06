package com.community.prime.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.community.prime.entity.Admin;
import com.community.prime.utils.AdminHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.community.prime.utils.RedisConstants.LOGIN_ADMIN_KEY;
import static com.community.prime.utils.RedisConstants.LOGIN_ADMIN_TTL;

/**
 * 管理员登录拦截器
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取请求头中的token
        String token = request.getHeader("admin-token");
        
        // 检查是否是登录请求，登录请求不需要token
        String uri = request.getRequestURI();
        if (uri != null && (uri.endsWith("/admin/login") || uri.contains("/admin/login"))) {
            return true;
        }
        
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 2. 基于token获取Redis中的管理员
        String key = LOGIN_ADMIN_KEY + token;
        Map<Object, Object> adminMap = stringRedisTemplate.opsForHash().entries(key);

        // 3. 判断管理员是否存在
        if (adminMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 4. 将查询到的Hash数据转为Admin对象
        Admin admin = BeanUtil.fillBeanWithMap(adminMap, new Admin(), false);

        // 5. 将管理员信息存入ThreadLocal
        AdminHolder.saveAdmin(admin);
        AdminHolder.saveToken(token);

        // 6. 刷新token有效期
        stringRedisTemplate.expire(key, LOGIN_ADMIN_TTL, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 移除ThreadLocal中的管理员信息
        AdminHolder.removeAdmin();
    }
}
