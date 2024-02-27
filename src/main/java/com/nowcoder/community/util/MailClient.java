package com.nowcoder.community.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
@Slf4j
public class MailClient {
    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String from;

    public void sentMail(String to, String subject, String content) {
        try {
            //创建发送消息的对象
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            //借助于helper对象来完善消息对象的信息
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
            //邮件来源者
            mimeMessageHelper.setFrom(from);
            //目标邮箱
            mimeMessageHelper.setTo(to);
            //邮件主题
            mimeMessageHelper.setSubject(subject);
            //邮件内容，这里第二个参数为true代表可以识别html的语法邮件内容
            mimeMessageHelper.setText(content,true);
            javaMailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (MessagingException e) {
            log.error("发送邮件发生错误了，错误信息为:" + e.getMessage());
        }

    }
}
