package com.codeclinic.user.vo;

import com.codeclinic.user.entity.User;

public record UserVO(Long id, String username, String email, String displayName) {
    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName());
    }
}
