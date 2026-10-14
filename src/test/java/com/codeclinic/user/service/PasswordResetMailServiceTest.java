package com.codeclinic.user.service;

import com.codeclinic.common.BusinessException;
import com.codeclinic.user.config.PasswordResetProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetMailServiceTest {

    @Test
    void sendsVerificationCodeToRegisteredEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setMailFrom("sender@example.com");
        properties.setCodeExpiryMinutes(10);
        PasswordResetMailService service = new PasswordResetMailService(provider, properties);

        service.send("receiver@example.com", "123456");

        var messageCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertArrayEquals(new String[]{"receiver@example.com"}, message.getTo());
        assertEquals("sender@example.com", message.getFrom());
        assertEquals("CodeClinic密码重置验证码", message.getSubject());
        assertTrue(message.getText().contains("123456"));
        assertTrue(message.getText().contains("10分钟内有效"));
    }

    @Test
    void rejectsRequestWhenMailSenderIsNotConfigured() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        PasswordResetMailService service = new PasswordResetMailService(
                provider, new PasswordResetProperties());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.send("receiver@example.com", "123456"));
        assertTrue(exception.getMessage().contains("发件邮箱"));
        verify(provider).getIfAvailable();
    }

    @Test
    void configuresQqMailForCurrentRunAndUsesIt() throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSenderImpl runtimeSender = mock(JavaMailSenderImpl.class);
        PasswordResetMailService service = new PasswordResetMailService(
                provider, new PasswordResetProperties()) {
            @Override
            JavaMailSenderImpl createRuntimeMailSender(
                    String email, String authorizationCode, SmtpSettings settings) {
                assertEquals("sender@qq.com", email);
                assertEquals("smtp.qq.com", settings.host());
                assertEquals(465, settings.port());
                assertTrue(settings.ssl());
                return runtimeSender;
            }
        };

        service.configureForCurrentRun("Sender@QQ.com", "authorization-code");
        service.send("receiver@example.com", "654321");

        verify(runtimeSender).testConnection();
        var messageCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(runtimeSender).send(messageCaptor.capture());
        assertEquals("sender@qq.com", messageCaptor.getValue().getFrom());
        assertTrue(messageCaptor.getValue().getText().contains("654321"));
    }

    @Test
    void rejectsUnsupportedProviderBeforeUsingAuthorizationCode() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        PasswordResetMailService service = new PasswordResetMailService(
                provider, new PasswordResetProperties());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.configureForCurrentRun("sender@example.com", "secret"));

        assertTrue(exception.getMessage().contains("暂不支持"));
    }
}
