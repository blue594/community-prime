package com.community.prime.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.prime.dto.LoginFormDTO;
import com.community.prime.dto.UserDTO;
import com.community.prime.entity.User;
import com.community.prime.vo.Result;

import javax.servlet.http.HttpSession;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 发送短信验证码
     * 
     * @param phone   手机号
     * @param session HttpSession
     * @return 结果
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 短信验证码登录
     * 
     * @param loginForm 登录表单
     * @param session   HttpSession
     * @return 登录结果
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 获取当前登录用户信息
     * 
     * @return 用户信息
     */
    Result me();

    /**
     * 根据ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户DTO
     */
    UserDTO queryUserById(Long userId);

    /**
     * 用户注册
     *
     * @param phone    手机号
     * @param password 密码
     * @return 注册结果
     */
    Result register(String phone, String password);

    /**
     * 更新用户头像
     *
     * @param icon 头像URL
     * @return 更新结果
     */
    Result updateIcon(String icon);

    /**
     * 更新用户昵称
     *
     * @param nickName 昵称
     * @return 更新结果
     */
    Result updateNickName(String nickName);
}
