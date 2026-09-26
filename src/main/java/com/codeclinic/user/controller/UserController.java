package com.codeclinic.user.controller;

import com.codeclinic.common.ApiResult;
import com.codeclinic.user.dto.LoginRequest;
import com.codeclinic.user.dto.PasswordResetCodeRequest;
import com.codeclinic.user.dto.PasswordResetRequest;
import com.codeclinic.user.dto.RegisterRequest;
import com.codeclinic.user.service.UserService;
import com.codeclinic.user.vo.LoginVO;
import com.codeclinic.user.vo.PasswordResetCodeVO;
import com.codeclinic.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

    @PostMapping("/password/reset")
    public ApiResult<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request);
        return ApiResult.successMessage("密码已重置");
    }

    @GetMapping("/info")
    public ApiResult<UserVO> info() {
        return ApiResult.success(userService.current());
    }
}
