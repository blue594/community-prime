package com.community.prime.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.community.prime.dto.UserDTO;
import com.community.prime.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.community.prime.utils.RedisConstants.LOGIN_USER_KEY;
import static com.community.prime.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 【面试重点】登录拦截器 - Token认证与ThreadLocal应用
 * 
 * 【面试点 1】基于Token的认证流程
 * 1. 从请求头获取token
 * 2. 使用token作为key从Redis查询用户信息
 * 3. 用户存在则保存到ThreadLocal并放行
 * 4. 用户不存在返回401
 * 
 * 【面试点 2】Token续期机制（自动刷新）
 * - 每次请求都刷新Redis中token的过期时间
 * - 实现"30分钟无操作自动过期，有操作则续期"
 * 
 * 【面试点 3】ThreadLocal使用要点
 * - 在preHandle中存入用户信息
 * - 在afterCompletion中必须remove，防止内存泄漏
 * - 适用于同一线程内的数据共享，避免方法间传递参数
 * 
 * 【面试点 4】拦截器配置
 * - 在WebMvcConfig中配置拦截路径和排除路径
 * - 登录、注册等接口需要排除拦截
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 【面试点】请求处理前执行 - 身份认证
     * 
     * 流程：
     * 1. 从请求头获取token
     * 2. Redis查询用户信息
     * 3. 存入ThreadLocal
     * 4. 刷新token有效期（实现自动续期）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取请求路径
        String uri = request.getRequestURI();
        
        // 如果请求的是admin接口，直接放行，由AdminInterceptor处理
        if (uri != null && uri.contains("/admin/")) {
            return true;
        }
        
        // 1. 获取请求头中的token
        String token = request.getHeader("authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 2. 基于token获取Redis中的用户
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

        // 3. 判断用户是否存在
        if (userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 4. 将查询到的Hash数据转为UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        // 【面试点】5. 将用户信息和token存入ThreadLocal
        // 后续Controller、Service可以直接通过UserHolder获取用户信息
        UserHolder.saveUser(userDTO);
        UserHolder.saveToken(token);

        // 【面试点】6. 刷新token有效期（自动续期机制）
        // 用户持续操作则token一直有效，30分钟无操作自动过期
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;
    }

    /**
     * 【面试点】请求完成后执行 - 清理ThreadLocal
     * 
     * 必须remove的原因：
     * - Tomcat使用线程池处理请求，线程会复用
     * - 如果不remove，下次请求可能获取到上次请求的用户信息
     * - 长期不remove会导致内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 移除ThreadLocal中的用户，防止内存泄漏
        UserHolder.removeUser();
    }
}
