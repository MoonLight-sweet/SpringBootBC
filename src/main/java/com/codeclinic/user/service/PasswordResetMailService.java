package com.codeclinic.user.service;

import com.codeclinic.common.BusinessException;
import com.codeclinic.user.config.PasswordResetProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import java.util.Locale;
import java.util.Properties;

@Service
public class PasswordResetMailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordResetProperties properties;
    private volatile JavaMailSender runtimeMailSender;
    private volatile String runtimeMailFrom;

    public PasswordResetMailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    PasswordResetProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    public void send(String email, String code) {
        JavaMailSender mailSender = runtimeMailSender;
        if (mailSender == null) {
            mailSender = mailSenderProvider.getIfAvailable();
        }
        if (mailSender == null) {
            throw new BusinessException("邮件服务未配置，请先完成本机发件邮箱设置");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        String mailFrom = StringUtils.hasText(runtimeMailFrom) ? runtimeMailFrom : properties.getMailFrom();
        if (StringUtils.hasText(mailFrom)) {
            message.setFrom(mailFrom);
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

    public synchronized void configureForCurrentRun(String email, String authorizationCode) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        SmtpSettings settings = settingsFor(normalizedEmail);
        JavaMailSenderImpl candidate = createRuntimeMailSender(
                normalizedEmail, authorizationCode.trim(), settings);
        try {
            candidate.testConnection();
        } catch (MessagingException | RuntimeException e) {
            throw new BusinessException("邮箱配置验证失败，请确认已开启SMTP服务并填写正确的邮箱授权码");
        }
        runtimeMailSender = candidate;
        runtimeMailFrom = normalizedEmail;
    }

    JavaMailSenderImpl createRuntimeMailSender(String email, String authorizationCode, SmtpSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.host());
        mailSender.setPort(settings.port());
        mailSender.setUsername(email);
        mailSender.setPassword(authorizationCode);
        mailSender.setDefaultEncoding("UTF-8");

        Properties javaMailProperties = mailSender.getJavaMailProperties();
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.smtp.connectiontimeout", "8000");
        javaMailProperties.put("mail.smtp.timeout", "8000");
        javaMailProperties.put("mail.smtp.writetimeout", "8000");
        if (settings.ssl()) {
            javaMailProperties.put("mail.smtp.ssl.enable", "true");
            javaMailProperties.put("mail.smtp.ssl.trust", settings.host());
        }
        if (settings.startTls()) {
            javaMailProperties.put("mail.smtp.starttls.enable", "true");
            javaMailProperties.put("mail.smtp.starttls.required", "true");
        }
        return mailSender;
    }

    static SmtpSettings settingsFor(String email) {
        int at = email.lastIndexOf('@');
        String domain = at >= 0 ? email.substring(at + 1).toLowerCase(Locale.ROOT) : "";
        return switch (domain) {
            case "qq.com" -> new SmtpSettings("smtp.qq.com", 465, true, false);
            case "163.com" -> new SmtpSettings("smtp.163.com", 465, true, false);
            case "126.com" -> new SmtpSettings("smtp.126.com", 465, true, false);
            case "gmail.com", "googlemail.com" ->
                    new SmtpSettings("smtp.gmail.com", 587, false, true);
            case "outlook.com", "hotmail.com", "live.com" ->
                    new SmtpSettings("smtp.office365.com", 587, false, true);
            default -> throw new BusinessException(
                    "该邮箱暂不支持页面自动配置，请按README使用环境变量配置SMTP服务");
        };
    }

    record SmtpSettings(String host, int port, boolean ssl, boolean startTls) {
    }
}
