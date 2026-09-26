package com.codeclinic.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("password_reset_code")
public class PasswordResetCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String codeHash;
    private LocalDateTime expireTime;
    private Boolean used;
    private LocalDateTime createTime;
}
