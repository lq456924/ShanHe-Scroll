package com.mytravel.user;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("MyTravel - 邮箱验证码");
        message.setText("您的验证码是：" + code + "\n\n" +
                "5分钟内有效，请勿泄露给他人。\n\n" +
                "如果不是您本人操作，请忽略此邮件。");
        mailSender.send(message);
    }
}
