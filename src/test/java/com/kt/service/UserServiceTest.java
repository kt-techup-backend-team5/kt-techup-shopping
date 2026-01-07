package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.user.Gender;
import com.kt.domain.user.Role;
import com.kt.domain.user.User;
import com.kt.dto.user.AdminChangePasswordRequest;
import com.kt.dto.user.UserChangeRequest;
import com.kt.dto.user.UserChangePasswordRequest;
import com.kt.dto.user.UserCreateRequest;
import com.kt.repository.user.UserRepository;
import com.kt.security.DefaultCurrentUser;
import com.kt.security.TechUpAuthenticationToken;
import com.kt.support.fixture.UserFixture;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
class UserServiceTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MailCheckService mailCheckService;

    @BeforeEach
    void setUp() {
        given(mailCheckService.isVerifiedEmail(anyString())).willReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("회원_생성_성공")
    void 회원_생성_성공() {
        // given
        UserCreateRequest request = createRequest("login_1", "user1@test.com");

        // when
        userService.create(request);

        // then
        var saved = userRepository.findByLoginId("login_1").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("user1@test.com");
        assertThat(passwordEncoder.matches(request.password(), saved.getPassword())).isTrue();
        verify(mailCheckService).clearVerifiedEmail("user1@test.com");
    }

    @Test
    @DisplayName("회원_생성_실패_이메일_인증_미완료")
    void 회원_생성_실패_이메일_인증_미완료() {
        // given
        UserCreateRequest request = createRequest("login_1", "user1@test.com");
        given(mailCheckService.isVerifiedEmail("user1@test.com")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.create(request))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.AUTH_EMAIL_UNVERIFIED.getMessage());
    }

    @Test
    @DisplayName("회원_생성_실패_중복_아이디")
    void 회원_생성_실패_중복_아이디() {
        // given
        userRepository.save(UserFixture.defaultCustomer());
        UserCreateRequest request = createRequest("test_user", "unique@test.com");

        // when & then
        assertThatThrownBy(() -> userService.create(request))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.ALREADY_EXISTS_USER_ID.getMessage());
    }

    @Test
    @DisplayName("회원_생성_실패_중복_이메일")
    void 회원_생성_실패_중복_이메일() {
        // given
        userRepository.save(UserFixture.defaultCustomer());
        UserCreateRequest request = createRequest("unique_login", "customer1@test.com");

        // when & then
        assertThatThrownBy(() -> userService.create(request))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.ALREADY_EXISTS_EMAIL.getMessage());
    }

    @Test
    @DisplayName("로그인아이디_찾기")
    void 로그인아이디_찾기() {
        // given
        var user = userRepository.save(UserFixture.defaultCustomer());

        // when
        var loginId = userService.findLoginId(user.getName(), user.getEmail());

        // then
        assertThat(loginId).isEqualTo(user.getLoginId());
    }

    @Test
    @DisplayName("회원_검색_키워드없음")
    void 회원_검색_키워드없음() {
        // given
        userRepository.save(UserFixture.defaultCustomer());
        userRepository.save(UserFixture.customer("login_search", "search@test.com"));

        // when
        var result = userService.search(PageRequest.of(0, 10), null);

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("회원_검색_키워드포함")
    void 회원_검색_키워드포함() {
        // given
        userRepository.save(UserFixture.defaultCustomer());
        var keywordUser = UserFixture.customer("keyword_user", "keyword@test.com");
        keywordUser.update("키워드 사용자", "keyword@test.com", "010-1111-3333");
        userRepository.save(keywordUser);

        // when
        var result = userService.search(PageRequest.of(0, 10), "키워드");

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("키워드 사용자");
    }

    @Test
    @DisplayName("비밀번호_변경_성공")
    void 비밀번호_변경_성공() {
        // given
        UserCreateRequest request = createRequest("login_2", "user2@test.com");
        userService.create(request);
        var user = userRepository.findByLoginId("login_2").orElseThrow();

        // when
        userService.changePassword(user.getId(),
            new UserChangePasswordRequest("Password1234!", "NewPass123!", "NewPass123!"));

        // then
        var updated = userRepository.findByIdOrThrow(user.getId());
        assertThat(passwordEncoder.matches("NewPass123!", updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호_변경_실패_기존비밀번호_불일치")
    void 비밀번호_변경_실패_기존비밀번호_불일치() {
        // given
        UserCreateRequest request = createRequest("login_3", "user3@test.com");
        userService.create(request);
        var user = userRepository.findByLoginId("login_3").orElseThrow();

        // when & then
        assertThatThrownBy(() -> userService.changePassword(user.getId(),
            new UserChangePasswordRequest("WrongPass123!", "NewPass123!", "NewPass123!")))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.DOES_NOT_MATCH_OLD_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("비밀번호_변경_실패_확인비밀번호_불일치")
    void 비밀번호_변경_실패_확인비밀번호_불일치() {
        // given
        UserCreateRequest request = createRequest("login_4", "user4@test.com");
        userService.create(request);
        var user = userRepository.findByLoginId("login_4").orElseThrow();

        // when & then
        assertThatThrownBy(() -> userService.changePassword(user.getId(),
            new UserChangePasswordRequest("Password1234!", "NewPass123!", "NewPass124!")))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.NOT_MATCHED_CHECK_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("비밀번호_변경_실패_기존비밀번호와_동일")
    void 비밀번호_변경_실패_기존비밀번호와_동일() {
        // given
        UserCreateRequest request = createRequest("login_5", "user5@test.com");
        userService.create(request);
        var user = userRepository.findByLoginId("login_5").orElseThrow();

        // when & then
        assertThatThrownBy(() -> userService.changePassword(user.getId(),
            new UserChangePasswordRequest("Password1234!", "Password1234!", "Password1234!")))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.CAN_NOT_ALLOWED_SAME_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("관리자_비밀번호_변경_성공")
    void 관리자_비밀번호_변경_성공() {
        // given
        UserCreateRequest request = createRequest("login_6", "user6@test.com");
        userService.create(request);
        var user = userRepository.findByLoginId("login_6").orElseThrow();

        // when
        userService.changePasswordByAdmin(user.getId(), new AdminChangePasswordRequest("AdminPass123!"));

        // then
        var updated = userRepository.findByIdOrThrow(user.getId());
        assertThat(passwordEncoder.matches("AdminPass123!", updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("현재_사용자_조회")
    void 현재_사용자_조회() {
        // given
        var user = userRepository.save(UserFixture.defaultCustomer());
        setCurrentUser(user);

        // when
        var response = userService.getCurrentUserInfo();

        // then
        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.email()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("현재_사용자_정보_변경")
    void 현재_사용자_정보_변경() {
        // given
        var user = userRepository.save(UserFixture.defaultCustomer());
        setCurrentUser(user);
        UserChangeRequest request = new UserChangeRequest("변경이름", "010-2222-3333", "changed@test.com");

        // when
        var response = userService.changeCurrentUser(request);

        // then
        assertThat(response.name()).isEqualTo("변경이름");
        assertThat(response.email()).isEqualTo("changed@test.com");
        assertThat(response.mobile()).isEqualTo("010-2222-3333");
    }

    @Test
    @DisplayName("회원정보_수정")
    void 회원정보_수정() {
        // given
        var user = userRepository.save(UserFixture.defaultCustomer());

        // when
        var response = userService.update(
            user.getId(),
            "변경된 이름",
            "changed@test.com",
            "010-1111-2222"
        );

        // then
        assertThat(response.name()).isEqualTo("변경된 이름");
        assertThat(response.email()).isEqualTo("changed@test.com");
        assertThat(response.mobile()).isEqualTo("010-1111-2222");
    }

    @Test
    @DisplayName("회원_상세_조회")
    void 회원_상세_조회() {
        // given
        var user = userRepository.save(UserFixture.defaultCustomer());

        // when
        var detail = userService.detail(user.getId());

        // then
        assertThat(detail.getId()).isEqualTo(user.getId());
        assertThat(detail.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("회원_탈퇴")
    void 회원_탈퇴() {
        // given
        var user = userRepository.save(UserFixture.defaultCustomer());

        // when
        userService.withdrawal(user.getId());

        // then
        var deleted = userRepository.findByIdIncludeDeletedOrThrow(user.getId());
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("회원_비활성화")
    void 회원_비활성화() {
        // given
        var user = userRepository.save(UserFixture.defaultCustomer());

        // when
        userService.deactivateUser(user.getId());

        // then
        var deactivated = userRepository.findByIdIncludeDeletedOrThrow(user.getId());
        assertThat(deactivated.isDeleted()).isTrue();
        assertThat(deactivated.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("회원_비활성화_후_활성화")
    void 회원_비활성화_후_활성화() {
        // given
        var user = userRepository.save(UserFixture.defaultCustomer());

        // when
        userService.deactivateUser(user.getId());

        // then
        var deactivated = userRepository.findByIdIncludeDeletedOrThrow(user.getId());
        assertThat(deactivated.isDeleted()).isTrue();

        // when
        userService.activateUser(user.getId());

        // then
        var activated = userRepository.findByIdIncludeDeletedOrThrow(user.getId());
        assertThat(activated.isDeleted()).isFalse();
        assertThat(activated.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("관리자_대상_조회_성공")
    void 관리자_대상_조회_성공() {
        // given
        var admin = userRepository.save(UserFixture.defaultAdmin());

        // when
        var target = userService.getAdminTargetOrThrow(admin.getId());

        // then
        assertThat(target.getId()).isEqualTo(admin.getId());
    }

    @Test
    @DisplayName("관리자_목록_조회")
    void 관리자_목록_조회() {
        // given
        userRepository.save(UserFixture.defaultAdmin());
        userRepository.save(superAdmin("super_admin", "super@test.com"));
        userRepository.save(UserFixture.defaultCustomer());

        // when
        var result = userService.searchAdmins(PageRequest.of(0, 10), null, null);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .allMatch(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN);
    }

    @Test
    @DisplayName("회원_목록_조회")
    void 회원_목록_조회() {
        // given
        userRepository.save(UserFixture.defaultAdmin());
        userRepository.save(UserFixture.defaultCustomer());
        userRepository.save(UserFixture.customer("customer2", "customer2@test.com"));

        // when
        var result = userService.searchCustomers(PageRequest.of(0, 10), null, null, false);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(user -> user.getRole() == Role.CUSTOMER);
    }

    @Test
    @DisplayName("관리자_대상_조회_실패_관리자아님")
    void 관리자_대상_조회_실패_관리자아님() {
        // given
        var customer = userRepository.save(UserFixture.defaultCustomer());

        // when & then
        assertThatThrownBy(() -> userService.getAdminTargetOrThrow(customer.getId()))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.USER_NOT_ADMIN.getMessage());
    }

    @Test
    @DisplayName("관리자_삭제_실패_자기자신")
    void 관리자_삭제_실패_자기자신() {
        // given
        var admin = userRepository.save(UserFixture.defaultAdmin());

        // when & then
        assertThatThrownBy(() -> userService.deleteAdmin(admin.getId(), admin.getId()))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.CANNOT_DELETE_SELF.getMessage());
    }

    @Test
    @DisplayName("관리자_삭제_실패_슈퍼관리자")
    void 관리자_삭제_실패_슈퍼관리자() {
        // given
        var admin = userRepository.save(UserFixture.defaultAdmin());
        var superAdmin = userRepository.save(superAdmin("super_admin", "super@test.com"));

        // when & then
        assertThatThrownBy(() -> userService.deleteAdmin(admin.getId(), superAdmin.getId()))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.CANNOT_DELETE_SUPER_ADMIN.getMessage());
    }

    @Test
    @DisplayName("관리자_삭제_실패_관리자아님")
    void 관리자_삭제_실패_관리자아님() {
        // given
        var admin = userRepository.save(UserFixture.defaultAdmin());
        var customer = userRepository.save(UserFixture.defaultCustomer());

        // when & then
        assertThatThrownBy(() -> userService.deleteAdmin(admin.getId(), customer.getId()))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.USER_NOT_ADMIN.getMessage());
    }

    @Test
    @DisplayName("관리자_삭제_성공")
    void 관리자_삭제_성공() {
        // given
        var current = userRepository.save(UserFixture.defaultAdmin());
        var target = userRepository.save(UserFixture.admin("admin_target", "target@test.com"));

        // when
        userService.deleteAdmin(current.getId(), target.getId());

        // then
        var deleted = userRepository.findByIdIncludeDeletedOrThrow(target.getId());
        assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("관리자_권한_부여")
    void 관리자_권한_부여() {
        // given
        var actor = userRepository.save(superAdmin("super_admin", "super@test.com"));
        var user = userRepository.save(UserFixture.defaultCustomer());
        var currentUser = new DefaultCurrentUser(actor.getId(), actor.getLoginId(), actor.getRole());

        // when
        userService.grantAdminRole(user.getId(), currentUser);

        // then
        var updated = userRepository.findByIdOrThrow(user.getId());
        assertThat(updated.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("관리자_권한_회수")
    void 관리자_권한_회수() {
        // given
        var actor = userRepository.save(superAdmin("super_admin", "super@test.com"));
        var admin = userRepository.save(UserFixture.defaultAdmin());
        var currentUser = new DefaultCurrentUser(actor.getId(), actor.getLoginId(), actor.getRole());

        // when
        userService.revokeAdminRole(admin.getId(), currentUser);

        // then
        var updated = userRepository.findByIdOrThrow(admin.getId());
        assertThat(updated.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("임시비밀번호_초기화")
    void 임시비밀번호_초기화() {
        // given
        UserCreateRequest request = createRequest("login_7", "user7@test.com");
        userService.create(request);
        var user = userRepository.findByLoginId("login_7").orElseThrow();

        // when
        var tempPassword = userService.initPassword(user.getId());

        // then
        var updated = userRepository.findByIdOrThrow(user.getId());
        assertThat(tempPassword).hasSize(8);
        assertThat(passwordEncoder.matches(tempPassword, updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("관리자_임시비밀번호_초기화_성공")
    void 관리자_임시비밀번호_초기화_성공() {
        // given
        var admin = userRepository.save(UserFixture.defaultAdmin());

        // when
        var tempPassword = userService.initAdminPassword(admin.getId());

        // then
        var updated = userRepository.findByIdOrThrow(admin.getId());
        assertThat(tempPassword).hasSize(8);
        assertThat(passwordEncoder.matches(tempPassword, updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("관리자_임시비밀번호_초기화_실패_관리자아님")
    void 관리자_임시비밀번호_초기화_실패_관리자아님() {
        // given
        var customer = userRepository.save(UserFixture.defaultCustomer());

        // when & then
        assertThatThrownBy(() -> userService.initAdminPassword(customer.getId()))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.USER_NOT_ADMIN.getMessage());
    }

    private UserCreateRequest createRequest(String loginId, String email) {
        return new UserCreateRequest(
            loginId,
            "Password1234!",
            "테스트 사용자",
            email,
            "010-1111-2222",
            Gender.MALE,
            LocalDate.now()
        );
    }

    private User superAdmin(String loginId, String email) {
        return new User(
            loginId,
            "Password1234!",
            "테스트 사용자",
            email,
            "010-1111-2222",
            Gender.MALE,
            LocalDate.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            Role.SUPER_ADMIN
        );
    }

    private void setCurrentUser(User user) {
        DefaultCurrentUser principal = new DefaultCurrentUser(user.getId(), user.getLoginId(), user.getRole());
        TechUpAuthenticationToken authentication =
            new TechUpAuthenticationToken(principal, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
