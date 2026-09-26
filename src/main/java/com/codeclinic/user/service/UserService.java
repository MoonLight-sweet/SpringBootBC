package com.codeclinic.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.codeclinic.common.BusinessException;
import com.codeclinic.security.CurrentUser;
import com.codeclinic.security.JwtService;
import com.codeclinic.user.dto.LoginRequest;
import com.codeclinic.user.dto.RegisterRequest;
import com.codeclinic.user.entity.User;
import com.codeclinic.user.mapper.UserMapper;
import com.codeclinic.user.vo.LoginVO;
import com.codeclinic.user.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    public UserVO current() {
        User user = userMapper.selectById(CurrentUser.id());
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return UserVO.from(user);
    }
}
