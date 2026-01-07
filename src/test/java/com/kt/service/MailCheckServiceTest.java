package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mail.javamail.JavaMailSender;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.support.fixture.UserFixture;

@ExtendWith(MockitoExtension.class)
class MailCheckServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailRedisService mailRedisService;

    @InjectMocks
    private MailCheckService mailCheckService;

    @BeforeEach
    void setUp() {
        setField("emailExpiration", 300L);
        setField("verifiedExpirationSeconds", 1800L);
        setField("fromAddress", "noreply@test.com");
    }

    @Test
    @DisplayName("이메일_인증번호_발송_성공")
    void 이메일_인증번호_발송_성공() {
        // given
        String email = UserFixture.defaultCustomer().getEmail();
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        given(mailSender.createMimeMessage()).willReturn(message);
        doNothing().when(mailSender).send(message);

        // when
        mailCheckService.sendMessage(email);

        // then
        verify(mailSender).send(message);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> expireCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mailRedisService).setDataExpire(keyCaptor.capture(), valueCaptor.capture(), expireCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("mail:auth:" + email);
        assertThat(valueCaptor.getValue()).matches("\\d{4}");
        assertThat(expireCaptor.getValue()).isEqualTo(300L);
    }

    @Test
    @DisplayName("이메일_인증번호_확인_성공_삭제처리")
    void 이메일_인증번호_확인_성공_삭제처리() {
        // given
        String email = UserFixture.defaultCustomer().getEmail();
        String authNum = "1234";
        given(mailRedisService.getData("mail:auth:" + email)).willReturn(authNum);

        // when
        boolean result = mailCheckService.checkAuthNum(email, authNum);

        // then
        assertThat(result).isTrue();
        verify(mailRedisService).deleteData("mail:auth:" + email);
    }

    @Test
    @DisplayName("이메일_인증번호_확인_실패")
    void 이메일_인증번호_확인_실패() {
        // given
        String email = UserFixture.defaultCustomer().getEmail();
        given(mailRedisService.getData("mail:auth:" + email)).willReturn("9999");

        // when
        boolean result = mailCheckService.checkAuthNum(email, "1234");

        // then
        assertThat(result).isFalse();
        verify(mailRedisService, never()).deleteData("mail:auth:" + email);
    }

    @Test
    @DisplayName("이메일_인증번호_검증_실패")
    void 이메일_인증번호_검증_실패() {
        // given
        String email = UserFixture.defaultCustomer().getEmail();
        given(mailRedisService.getData("mail:auth:" + email)).willReturn("9999");

        // when & then
        assertThatThrownBy(() -> mailCheckService.verifyAndMarkEmail(email, "1234"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_MATCHED_AUTHNUM.getMessage());
    }

    @Test
    @DisplayName("이메일_인증번호_검증_성공_인증상태저장")
    void 이메일_인증번호_검증_성공_인증상태저장() {
        // given
        String email = UserFixture.defaultCustomer().getEmail();
        String authNum = "1234";
        given(mailRedisService.getData("mail:auth:" + email)).willReturn(authNum);

        // when
        mailCheckService.verifyAndMarkEmail(email, authNum);

        // then
        verify(mailRedisService).deleteData("mail:auth:" + email);
        verify(mailRedisService).setDataExpire("mail:verified:" + email, "true", 1800L);
    }

    @Test
    @DisplayName("이메일_전송_실패시_인증번호_저장안함")
    void 이메일_전송_실패시_인증번호_저장안함() {
        // given
        String email = UserFixture.defaultCustomer().getEmail();
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        given(mailSender.createMimeMessage()).willReturn(message);
        doThrow(new RuntimeException("send fail")).when(mailSender).send(message);

        // when
        mailCheckService.sendMessage(email);

        // then
        verify(mailSender).send(message);
        verify(mailRedisService, never()).setDataExpire(eq("mail:auth:" + email), anyString(), eq(300L));
    }

    private void setField(String name, Object value) {
        try {
            Field field = MailCheckService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(mailCheckService, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("필드 설정 실패: " + name, e);
        }
    }
}
