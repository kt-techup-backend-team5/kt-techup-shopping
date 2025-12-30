package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.user.User;
import com.kt.dto.auth.AuthRequest;
import com.kt.repository.user.UserRepository;
import com.kt.security.JwtService;
import com.kt.support.fixture.UserFixture;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
class AuthServiceTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RedisService redisService;

    @Test
    @DisplayName("로그인_성공")
    @Tag("integration")
    void 로그인_성공() {
        // given
        var user = saveUser("login_user", "login@test.com", "Password1234!");

        // when
        var tokens = authService.login("login_user", "Password1234!");

        // then
        assertThat(tokens.getFirst()).isNotBlank();
        assertThat(tokens.getSecond()).isNotBlank();
        assertThat(jwtService.parseId(tokens.getFirst())).isEqualTo(user.getId());
        assertThat(redisService.findUserIdByRefreshToken(tokens.getSecond())).isEqualTo(user.getId());

        redisService.deleteRefreshToken(tokens.getSecond());
    }

    @Test
    @DisplayName("로그인_실패_아이디없음")
    void 로그인_실패_아이디없음() {
        assertThatThrownBy(() -> authService.login("missing", "Password1234!"))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.FAIL_LOGIN.getMessage());
    }

    @Test
    @DisplayName("로그인_실패_비밀번호불일치")
    void 로그인_실패_비밀번호불일치() {
        // given
        saveUser("login_user2", "login2@test.com", "Password1234!");

        // when & then
        assertThatThrownBy(() -> authService.login("login_user2", "WrongPass123!"))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.FAIL_LOGIN.getMessage());
    }

    @Test
    @DisplayName("리프레시토큰_삭제")
    @Tag("integration")
    void 리프레시토큰_삭제() {
        // given
        var user = saveUser("login_user3", "login3@test.com", "Password1234!");
        String refreshToken = jwtService.issue(user.getId(), jwtService.getRefreshExpiration());
        redisService.saveRefreshToken(refreshToken, user.getId(), 60L);

        // when
        authService.deleteRefreshToken(new AuthRequest.Logout(refreshToken));

        // then
        assertThat(redisService.findUserIdByRefreshToken(refreshToken)).isNull();
    }

    @Test
    @DisplayName("리프레시토큰_삭제_존재하지않는토큰")
    @Tag("integration")
    void 리프레시토큰_삭제_존재하지않는토큰() {
        // given
        String missingToken = "missing-token";

        // when
        authService.deleteRefreshToken(new AuthRequest.Logout(missingToken));

        // then
        assertThat(redisService.findUserIdByRefreshToken(missingToken)).isNull();
    }

    @Test
    @DisplayName("토큰_재발급_성공")
    @Tag("integration")
    void 토큰_재발급_성공() {
        // given
        var user = saveUser("login_user4", "login4@test.com", "Password1234!");
        String oldRefreshToken = jwtService.issue(user.getId(), jwtService.getRefreshExpiration());
        redisService.saveRefreshToken(oldRefreshToken, user.getId(), 60L);

        // when
        var reissued = authService.reissue(new AuthRequest.Reissue(oldRefreshToken));

        // then
        assertThat(reissued.accessToken()).isNotBlank();
        assertThat(reissued.refreshToken()).isNotBlank();
        assertThat(reissued.refreshToken()).isNotEqualTo(oldRefreshToken);
        assertThat(redisService.findUserIdByRefreshToken(oldRefreshToken)).isNull();
        assertThat(redisService.findUserIdByRefreshToken(reissued.refreshToken())).isEqualTo(user.getId());
        assertThat(jwtService.parseId(reissued.accessToken())).isEqualTo(user.getId());

        redisService.deleteRefreshToken(reissued.refreshToken());
    }

    @Test
    @DisplayName("토큰_재발급_실패_유효하지않은토큰")
    void 토큰_재발급_실패_유효하지않은토큰() {
        // when & then
        assertThatThrownBy(() -> authService.reissue(new AuthRequest.Reissue("invalid.token")))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.INVALID_JWT_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰_재발급_실패_만료된토큰")
    void 토큰_재발급_실패_만료된토큰() {
        // given
        String expiredToken = jwtService.issue(1L, new Date(System.currentTimeMillis() - 1000));

        // when & then
        assertThatThrownBy(() -> authService.reissue(new AuthRequest.Reissue(expiredToken)))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.EXPIRED_JWT_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰_재발급_실패_리프레시토큰_불일치")
    @Tag("integration")
    void 토큰_재발급_실패_리프레시토큰_불일치() {
        // given
        var user1 = saveUser("login_user5", "login5@test.com", "Password1234!");
        var user2 = saveUser("login_user6", "login6@test.com", "Password1234!");
        String refreshToken = jwtService.issue(user1.getId(), jwtService.getRefreshExpiration());
        redisService.saveRefreshToken(refreshToken, user2.getId(), 60L);

        // when & then
        assertThatThrownBy(() -> authService.reissue(new AuthRequest.Reissue(refreshToken)))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.INVALID_JWT_TOKEN.getMessage());

        redisService.deleteRefreshToken(refreshToken);
    }

    @Test
    @DisplayName("토큰_재발급_실패_리프레시토큰_미등록")
    @Tag("integration")
    void 토큰_재발급_실패_리프레시토큰_미등록() {
        // given
        var user = saveUser("login_user7", "login7@test.com", "Password1234!");
        String refreshToken = jwtService.issue(user.getId(), jwtService.getRefreshExpiration());

        // when & then
        assertThatThrownBy(() -> authService.reissue(new AuthRequest.Reissue(refreshToken)))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.INVALID_JWT_TOKEN.getMessage());
    }

    private User saveUser(String loginId, String email, String rawPassword) {
        var user = UserFixture.customer(loginId, email);
        user.changePassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }
}