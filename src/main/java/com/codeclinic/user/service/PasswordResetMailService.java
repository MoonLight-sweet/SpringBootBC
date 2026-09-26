package com.codeclinic.user.service;

import com.codeclinic.common.BusinessException;
import com.codeclinic.user.config.PasswordResetProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordResetMailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordResetProperties properties;

    public PasswordResetMailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    PasswordResetProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    public void send(String email, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new BusinessException("邮件服务未配置，请先设置发件邮箱和邮箱授权码");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(properties.getMailFrom())) {
            message.setFrom(properties.getMailFrom());
        }
        message.setTo(email);
        message.setSubject("CodeClinic密码重置验证码");
        message.setText("你的密码重置验证码是：" + code + "。验证码在"
                + properties.getCodeExpiryMinutes() + "分钟内有效，请勿转发给他人。");
        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            throw new BusinessException("验证码邮件发送失败，请稍后重试");
        }
    }
}
