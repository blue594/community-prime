package com.community.prime.controller;

import com.community.prime.dto.LoginFormDTO;
import com.community.prime.service.UserService;
import com.community.prime.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 用户控制器
 * 
 * 接口说明：
 * - POST /user/code    : 发送短信验证码
 * - POST /user/login   : 短信验证码登录
 * - GET  /user/me      : 获取当前登录用户信息
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 发送手机验证码
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     */
    @PostMapping("/login")
    public Result login(@RequestBody @Validated LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm, session);
    }

    /**
     * 获取当前登录的用户信息
     */
    @GetMapping("/me")
    public Result me() {
        return userService.me();
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@RequestParam("phone") String phone,
                          @RequestParam("password") String password) {
        return userService.register(phone, password);
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/upload-icon")
    public Result uploadIcon(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("请选择图片");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + suffix;

            String uploadDir = System.getProperty("user.dir") + "/uploads/user/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(uploadDir + filename);
            file.transferTo(dest);

            String url = "/uploads/user/" + filename;
            return Result.ok(url);

        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.fail("头像上传失败");
        }
    }

    /**
     * 更新用户头像
     */
    @PostMapping("/update-icon")
    public Result updateIcon(@RequestBody Map<String, String> params) {
        return userService.updateIcon(params.get("icon"));
    }

    /**
     * 更新用户昵称
     */
    @PostMapping("/update-nickname")
    public Result updateNickName(@RequestBody Map<String, String> params) {
        return userService.updateNickName(params.get("nickName"));
    }
}
