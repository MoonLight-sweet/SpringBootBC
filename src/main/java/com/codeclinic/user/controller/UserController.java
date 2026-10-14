package com.codeclinic.user.controller;

import com.codeclinic.common.ApiResult;
import com.codeclinic.common.BusinessException;
import com.codeclinic.user.dto.LoginRequest;
import com.codeclinic.user.dto.PasswordResetCodeRequest;
import com.codeclinic.user.dto.PasswordResetMailSetupRequest;
import com.codeclinic.user.dto.PasswordResetRequest;
import com.codeclinic.user.dto.RegisterRequest;
import com.codeclinic.user.service.PasswordResetMailService;
import com.codeclinic.user.service.UserService;
import com.codeclinic.user.vo.LoginVO;
import com.codeclinic.user.vo.PasswordResetCodeVO;
import com.codeclinic.user.vo.UserVO;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final PasswordResetMailService mailService;

    public UserController(UserService userService, PasswordResetMailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }

    @PostMapping("/register")
    public ApiResult<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.success(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success(userService.login(request));
    }

    @PostMapping("/password/reset-code")
    public ApiResult<PasswordResetCodeVO> requestPasswordResetCode(
            @Valid @RequestBody PasswordResetCodeRequest request) {
        return ApiResult.success(userService.requestPasswordResetCode(request));
    }

    @PostMapping("/password/mail-settings")
    public ApiResult<Void> configurePasswordResetMail(
            @Valid @RequestBody PasswordResetMailSetupRequest request,
            HttpServletRequest servletRequest) {
        if (!isLoopbackAddress(servletRequest.getRemoteAddr())) {
            throw new BusinessException(403, "发件邮箱只能在运行服务的本机配置");
        }
        mailService.configureForCurrentRun(request.email(), request.authorizationCode());
        return ApiResult.successMessage("发件邮箱配置成功");
    }

    @PostMapping("/password/reset")
    public ApiResult<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request);
        return ApiResult.successMessage("密码已重置");
    }

    @GetMapping("/info")
    public ApiResult<UserVO> info() {
        return ApiResult.success(userService.current());
    }

    static boolean isLoopbackAddress(String address) {
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
