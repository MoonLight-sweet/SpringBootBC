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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final LoginAttemptLimiter loginLimiter;
    private final PasswordResetRequestLimiter resetLimiter;
    private final JdbcTemplate jdbc;
    private final String dummyPasswordHash;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserMapper userMapper, PasswordResetCodeMapper resetCodeMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       PasswordResetMailService mailService, PasswordResetProperties resetProperties,
                       LoginAttemptLimiter loginLimiter, PasswordResetRequestLimiter resetLimiter,
                       JdbcTemplate jdbc) {
        this.userMapper = userMapper;
        this.resetCodeMapper = resetCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.resetProperties = resetProperties;
        this.loginLimiter = loginLimiter;
        this.resetLimiter = resetLimiter;
        this.jdbc = jdbc;
        this.dummyPasswordHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
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

    public LoginVO login(LoginRequest request, String remoteAddress) {
        String account = request.username().trim();
        loginLimiter.check(account, remoteAddress);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, account));
        boolean valid = passwordEncoder.matches(request.password(), user == null ? dummyPasswordHash : user.getPasswordHash());
        if (user == null || !valid) {
            loginLimiter.failed(account, remoteAddress);
            throw new BusinessException(401, "用户名或密码错误");
        }
        loginLimiter.succeeded(account);
        return new LoginVO(jwtService.createToken(user.getId(), user.getUsername(), user.getPasswordHash()), UserVO.from(user));
    }

    @Transactional
    public PasswordResetCodeVO requestPasswordResetCode(PasswordResetCodeRequest request, String remoteAddress) {
        String email = normalizeEmail(request.email());
        resetLimiter.acquire(email, remoteAddress);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        long expiresInSeconds = resetProperties.getCodeExpiryMinutes() * 60L;
        PasswordResetCodeVO response = new PasswordResetCodeVO(maskEmail(email), expiresInSeconds);
        if (user == null || !mailService.isConfigured()) {
            passwordEncoder.encode(String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000)));
            return response;
        }

        PasswordResetCode latest = resetCodeMapper.selectOne(Wrappers.<PasswordResetCode>lambdaQuery()
                .eq(PasswordResetCode::getUserId, user.getId())
                .orderByDesc(PasswordResetCode::getCreateTime)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (latest != null && latest.getCreateTime() != null
                && Duration.between(latest.getCreateTime(), now).getSeconds() < 60) {
            passwordEncoder.encode(String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000)));
            return response;
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
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mailService.sendAsync(email, code);
            }
        });
        return response;
    }

    @Transactional(noRollbackFor = BusinessException.class)
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
                .last("LIMIT 1 FOR UPDATE"));
        if (resetCode == null || resetCode.getExpireTime().isBefore(LocalDateTime.now())
                ) {
            throw new BusinessException("验证码无效或已过期");
        }
        jdbc.update("INSERT IGNORE INTO password_reset_attempt(reset_code_id) VALUES (?)", resetCode.getId());
        int allowed = jdbc.update("""
                UPDATE password_reset_attempt SET attempts = attempts + 1
                WHERE reset_code_id = ? AND attempts < 5
                """, resetCode.getId());
        if (allowed != 1 || !passwordEncoder.matches(request.code(), resetCode.getCodeHash())) {
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
