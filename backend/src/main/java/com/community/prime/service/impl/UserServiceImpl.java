package com.community.prime.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.prime.dto.LoginFormDTO;
import com.community.prime.dto.UserDTO;
import com.community.prime.entity.User;
import com.community.prime.handler.BusinessException;
import com.community.prime.mapper.UserMapper;
import com.community.prime.service.UserService;
import com.community.prime.utils.UserHolder;
import com.community.prime.vo.Result;
import com.community.prime.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.community.prime.utils.RedisConstants.*;

// todo
/**
 * 【面试重点】用户服务实现类 - Token认证与Redis应用
 * 
 * 【面试点 1】基于Token的无状态登录
 * - 服务端不保存session，使用Redis存储登录状态
 * - 客户端携带token，服务端通过token获取用户信息
 * - 优势：支持分布式部署，服务端无状态便于水平扩展
 * 
 * 【面试点 2】验证码存储策略
 * - 使用Redis存储验证码，设置2分钟过期时间
 * - key设计：login:code:手机号，便于快速查找和过期自动清理
 * 
 * 【面试点 3】登录状态存储
 * - 使用Redis Hash存储用户信息，便于单字段更新
 * - 设置30分钟过期时间，实现登录态自动过期
 * - key设计：login:token:token值
 * 
 * 【面试点 4】ThreadLocal使用
 * - 使用UserHolder（基于ThreadLocal）保存当前登录用户
 * - 避免在方法间传递user参数，代码更简洁
 * - 注意：请求结束后需要清理ThreadLocal，防止内存泄漏
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if (!isPhoneValid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 3. 保存验证码到Redis（替代session）
        stringRedisTemplate.opsForValue().set(
                LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 4. 发送验证码（模拟，实际项目接入短信服务商）
        log.info("发送短信验证码成功，手机号：{}，验证码：{}", phone, code);

        return Result.ok();
    }

    // todo
    /**
     * 【面试重点】登录实现 - 双模式登录（验证码/密码）
     * 
     * 【面试点】登录流程：
     * 1. 校验手机号格式
     * 2. 根据 loginType 判断登录方式
     *    - 密码登录：MD5加密后比对
     *    - 验证码登录：从Redis获取验证码校验
     * 3. 生成UUID作为token
     * 4. 用户信息存入Redis Hash，设置30分钟过期
     * 5. 返回token给客户端
     * 
     * 【面试点】Redis数据结构选择：
     * - 验证码：String类型，key=login:code:phone，过期2分钟
     * - 登录态：Hash类型，key=login:token:token，过期30分钟
     * 选择Hash存储用户信息便于单字段更新
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();

        // 1. 校验手机号
        if (!isPhoneValid(phone)) {
            return Result.fail("手机号格式错误");
        }

        User user = null;

        // 2. 判断登录类型
        if (loginForm.getLoginType() == 2) {
            // 【面试点】密码登录：MD5加密后比对（实际项目应使用更安全的加密方式如BCrypt）
            String password = loginForm.getPassword();
            if (password == null || password.isEmpty()) {
                return Result.fail("密码不能为空");
            }
            // 根据手机号查询用户
            user = query().eq("phone", phone).one();
            if (user == null) {
                return Result.fail("用户不存在");
            }
            // 校验密码
            String encryptedPassword = DigestUtil.md5Hex(password);
            if (!encryptedPassword.equals(user.getPassword())) {
                return Result.fail("密码错误");
            }
        } else {
            // 【面试点】验证码登录：从Redis获取并校验
            String code = loginForm.getCode();
            if (code == null || code.isEmpty()) {
                return Result.fail("验证码不能为空");
            }
            // 从Redis获取验证码并校验
            String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
            if (cacheCode == null || !cacheCode.equals(code)) {
                return Result.fail("验证码错误");
            }
            // 删除验证码（一次性使用）
            stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);

            // 根据手机号查询用户
            user = query().eq("phone", phone).one();
            // 判断用户是否存在
            if (user == null) {
                // 不存在，创建新用户
                user = createUserWithPhone(phone);
            }
        }

        // 【面试点】3. 保存用户信息到Redis（替代session，实现无状态登录）
        // 3.1 生成token
        String token = UUID.randomUUID().toString(true);
        // 3.2 将User对象转为Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString()));
        // 3.3 存储到Redis
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 3.4 设置token有效期（30分钟）
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 4. 返回token
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setPhone(phone.substring(0, 3) + "****" + phone.substring(7));
        userVO.setNickName(user.getNickName());
        userVO.setIcon(user.getIcon());
        userVO.setToken(token);

        return Result.ok(userVO);
    }

    @Override
    public Result me() {
        // 从ThreadLocal获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return Result.ok(user);
    }

    @Override
    public UserDTO queryUserById(Long userId) {
        // 查询用户详情
        User user = getById(userId);
        if (user == null) {
            return null;
        }
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        return userDTO;
    }

    /**
     * 根据手机号创建用户
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_" + RandomUtil.randomString(10));
        // 设置默认密码（手机号后6位）
        String defaultPassword = phone.substring(phone.length() - 6);
        user.setPassword(DigestUtil.md5Hex(defaultPassword));
        save(user);
        return user;
    }

    @Override
    public Result register(String phone, String password) {
        // 1. 校验手机号
        if (!isPhoneValid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 2. 校验密码
        if (password == null || password.length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }

        // 3. 检查手机号是否已注册
        User existUser = query().eq("phone", phone).one();
        if (existUser != null) {
            return Result.fail("手机号已注册");
        }

        // 4. 创建用户
        User user = new User();
        user.setPhone(phone);
        user.setPassword(DigestUtil.md5Hex(password));
        user.setNickName("user_" + RandomUtil.randomString(10));
        save(user);

        return Result.ok("注册成功");
    }

    @Override
    public Result updateIcon(String icon) {
        // 1. 获取当前登录用户
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 2. 更新头像
        User user = getById(userDTO.getId());
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setIcon(icon);
        updateById(user);

        // 3. 更新Redis中的用户信息
        String token = UserHolder.getToken();
        if (token != null) {
            String tokenKey = LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().put(tokenKey, "icon", icon);
        }

        // 4. 更新ThreadLocal中的用户信息
        userDTO.setIcon(icon);

        log.info("用户{}更新头像成功", userDTO.getId());
        // 返回更新后的用户信息
        return Result.ok(userDTO);
    }

    @Override
    public Result updateNickName(String nickName) {
        // 1. 获取当前登录用户
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 2. 校验昵称
        if (nickName == null || nickName.trim().isEmpty()) {
            return Result.fail("昵称不能为空");
        }
        if (nickName.length() > 20) {
            return Result.fail("昵称长度不能超过20个字符");
        }

        // 3. 更新昵称
        User user = getById(userDTO.getId());
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setNickName(nickName.trim());
        updateById(user);

        // 4. 更新Redis中的用户信息
        String token = UserHolder.getToken();
        if (token != null) {
            String tokenKey = LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().put(tokenKey, "nickName", nickName.trim());
        }

        // 5. 更新ThreadLocal中的用户信息
        userDTO.setNickName(nickName.trim());

        log.info("用户{}更新昵称成功: {}", userDTO.getId(), nickName.trim());
        // 返回更新后的用户信息
        return Result.ok(userDTO);
    }

    /**
     * 校验手机号格式
     */
    private boolean isPhoneValid(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}
