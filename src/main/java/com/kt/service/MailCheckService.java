package com.kt.service;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailCheckService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTH_KEY_PREFIX = "mail:auth:";
    private static final String VERIFIED_KEY_PREFIX = "mail:verified:";
    private final JavaMailSender mailSender;
    private final MailRedisService mailRedisService;

    @Value("${app.mail.expiration-seconds}")
    private long emailExpiration;

    @Value("${app.mail.verified-expiration-seconds}")
    private long verifiedExpirationSeconds;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async("mailTaskExecutor")
    public void sendMessage(String sendMail) {
        String authNum = createCode();

        String title = "[케클벅스]회원가입 이메일 인증 메일입니다.";
        String plainContent = "인증번호는 " + authNum + " 입니다.";
        String htmlContent = buildHtmlContent(authNum);
        try {
            createMessage(fromAddress, sendMail, title, plainContent, htmlContent);
            mailRedisService.setDataExpire(authKey(sendMail), authNum, emailExpiration);
        } catch (RuntimeException e) {
            log.error("이메일 전송 실패 - email: {}", sendMail, e);
        }

    }

    public String createCode() {
        return String.valueOf(1000 + SECURE_RANDOM.nextInt(9000));
    }

    public void createMessage(String from, String to, String title, String plainContent, String htmlContent) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "utf-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(plainContent, htmlContent);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new CustomException(ErrorCode.MAIL_DELIVERY_FAILED);
        }
    }

    public boolean checkAuthNum(String email, String authNum) {
        String storedAuthNum = mailRedisService.getData(authKey(email));
        boolean matches = authNum.equals(storedAuthNum);
        if (matches) {
            mailRedisService.deleteData(authKey(email));
        }
        return matches;
    }

    public void verifyAndMarkEmail(String email, String authNum) {
        if (!checkAuthNum(email, authNum)) {
            throw new CustomException(ErrorCode.NOT_MATCHED_AUTHNUM);
        }
        mailRedisService.setDataExpire(verifiedKey(email), "true", verifiedExpirationSeconds);
    }

    public boolean isVerifiedEmail(String email) {
        return "true".equals(mailRedisService.getData(verifiedKey(email)));
    }

    public void clearVerifiedEmail(String email) {
        mailRedisService.deleteData(verifiedKey(email));
    }

    private String authKey(String email) {
        return AUTH_KEY_PREFIX + email;
    }

    private String verifiedKey(String email) {
        return VERIFIED_KEY_PREFIX + email;
    }

    private String buildHtmlContent(String authNum) {
        String template = loadTemplate("templates/mail/mail-template.html");
        return template.replace("{{code}}", authNum);
    }

    private String loadTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ERROR_SYSTEM);
        }
    }
}
