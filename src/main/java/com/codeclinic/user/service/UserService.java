package com.codeclinic.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.codeclinic.common.BusinessException;
import com.codeclinic.security.CurrentUser;
import com.codeclinic.security.JwtService;
import com.codeclinic.user.dto.LoginRequest;
import com.codeclinic.user.dto.PasswordResetCodeRequest;
import com.codeclinic.user.dto.PasswordResetRequest;
import com.codeclinic.user.dto.RegisterRequest;
import com.codeclinic.user.config.PasswordResetProperties;
import com.codeclinic.user.entity.PasswordResetCode;
import com.codeclinic.user.entity.User;
import com.codeclinic.user.mapper.PasswordResetCodeMapper;
import com.codeclinic.user.mapper.UserMapper;
import com.codeclinic.user.vo.LoginVO;
import com.codeclinic.user.vo.PasswordResetCodeVO;
import com.codeclinic.user.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordResetCodeMapper resetCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetMailService mailService;
    private final PasswordResetProperties resetProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserMapper userMapper, PasswordResetCodeMapper resetCodeMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       PasswordResetMailService mailService, PasswordResetProperties resetProperties) {
        this.userMapper = userMapper;
        this.resetCodeMapper = resetCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.resetProperties = resetProperties;
    }

    @Transactional
    public UserVO register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (username.length() < 3) throw new BusinessException("用户名至少需要3个非空字符");
        long duplicate = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username).or().eq(User::getEmail, email));
        if (duplicate > 0) {
            throw new BusinessException("用户名或邮箱已被使用");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(email);
        user.setDisplayName(request.displayName());
        userMapper.insert(user);
        return UserVO.from(user);
    }

    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return new LoginVO(jwtService.createToken(user.getId(), user.getUsername()), UserVO.from(user));
    }

    @Transactional
    public PasswordResetCodeVO requestPasswordResetCode(PasswordResetCodeRequest request) {
        String email = normalizeEmail(request.email());
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        long expiresInSeconds = resetProperties.getCodeExpiryMinutes() * 60L;
        if (user == null) {
            return new PasswordResetCodeVO(maskEmail(email), expiresInSeconds);
        }

        PasswordResetCode latest = resetCodeMapper.selectOne(Wrappers.<PasswordResetCode>lambdaQuery()
                .eq(PasswordResetCode::getUserId, user.getId())
                .orderByDesc(PasswordResetCode::getCreateTime)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (latest != null && latest.getCreateTime() != null
                && Duration.between(latest.getCreateTime(), now).getSeconds() < 60) {
            throw new BusinessException("验证码发送过于频繁，请1分钟后再试");
        }

        resetCodeMapper.update(null, Wrappers.<PasswordResetCode>lambdaUpdate()
                .eq(PasswordResetCode::getUserId, user.getId())
                .eq(PasswordResetCode::getUsed, false)
                .set(PasswordResetCode::getUsed, true));

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setUserId(user.getId());
        resetCode.setCodeHash(passwordEncoder.encode(code));
        resetCode.setExpireTime(now.plusMinutes(resetProperties.getCodeExpiryMinutes()));
        resetCode.setUsed(false);
        resetCode.setCreateTime(now);
        resetCodeMapper.insert(resetCode);
        mailService.send(email, code);

        return new PasswordResetCodeVO(maskEmail(email), expiresInSeconds);
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String email = normalizeEmail(request.email());
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException("验证码无效或已过期");
        }
        PasswordResetCode resetCode = resetCodeMapper.selectOne(Wrappers.<PasswordResetCode>lambdaQuery()
                .eq(PasswordResetCode::getUserId, user.getId())
                .eq(PasswordResetCode::getUsed, false)
                .orderByDesc(PasswordResetCode::getCreateTime)
                .last("LIMIT 1"));
        if (resetCode == null || resetCode.getExpireTime().isBefore(LocalDateTime.now())
                || !passwordEncoder.matches(request.code(), resetCode.getCodeHash())) {
            throw new BusinessException("验证码无效或已过期");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        resetCode.setUsed(true);
        resetCodeMapper.updateById(resetCode);
    }

    public UserVO current() {
        User user = userMapper.selectById(CurrentUser.id());
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return UserVO.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}
