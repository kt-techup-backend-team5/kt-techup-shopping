package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kt.common.exception.ErrorCode;
import com.kt.dto.address.AddressRequest;
import com.kt.repository.address.AddressRepository;
import com.kt.repository.user.UserRepository;
import com.kt.support.fixture.UserFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
class AddressServiceTest {
    @Autowired
    private AddressService addressService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        var user = userRepository.save(UserFixture.defaultCustomer());
        userId = user.getId();
    }

    @Test
    @DisplayName("첫 배송지 등록 시 기본배송지로 자동 지정된다")
    void 첫_배송지_등록_기본배송지_자동지정() {
        // when
        var created = addressService.create(userId, req("집", false, "주소1"));

        // then
        assertThat(created.isDefault()).isTrue();
        assertThat(created.getAlias()).isEqualTo("집");
    }

    @Test
    @DisplayName("두번째 배송지 등록(isDefault=false) 시 기본배송지가 유지된다")
    void 두번째_배송지_등록_기본유지() {
        // given
        var first = addressService.create(userId, req("집", false, "주소1"));

        // when
        var second = addressService.create(userId, req("회사", false, "주소2"));

        // then
        var list = addressService.list(userId);
        assertThat(list).hasSize(2);
        assertThat(list.stream().filter(a -> a.isDefault()).count()).isEqualTo(1);
        assertThat(list.stream().filter(a -> a.isDefault()).findFirst().orElseThrow().getId())
            .isEqualTo(first.getId());
        assertThat(second.isDefault()).isFalse();
    }

    @Test
    @DisplayName("배송지 등록(isDefault=true) 시 기존 기본배송지가 해제되고 신규가 기본이 된다")
    void 배송지_등록_기본변경() {
        // given
        var first = addressService.create(userId, req("집", false, "주소1"));

        // when
        var second = addressService.create(userId, req("회사", true, "주소2"));

        // then
        var list = addressService.list(userId);
        assertThat(list.stream().filter(a -> a.isDefault()).count()).isEqualTo(1);
        assertThat(list.stream().filter(a -> a.isDefault()).findFirst().orElseThrow().getId())
            .isEqualTo(second.getId());

        var reloadedFirst = addressRepository.findAllByUserId(userId).stream()
            .filter(a -> a.getId().equals(first.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(reloadedFirst.isDefault()).isFalse();
    }

    @Test
    @DisplayName("기본배송지는 isDefault=false로 단독 해제할 수 없다")
    void 기본배송지_단독해제_불가() {
        // given
        var created = addressService.create(userId, req("집", false, "주소1"));

        // when & then
        assertThatThrownBy(() -> addressService.update(userId, created.getId(), req("집", false, "주소1-수정")))
            .isInstanceOf(com.kt.common.exception.CustomException.class)
            .hasMessage(ErrorCode.CANNOT_UNSET_DEFAULT_ADDRESS.getMessage());
    }

    @Test
    @DisplayName("기본배송지 삭제 시 남은 배송지 중 최신 1건이 기본배송지로 승격된다")
    void 기본배송지_삭제시_승격() throws Exception {
        // given
        var first = addressService.create(userId, req("집", false, "주소1"));

        // createdAt 동일 타임스탬프 방지(승격 기준이 createdAt인 경우 안정화)
        Thread.sleep(5);

        var second = addressService.create(userId, req("회사", false, "주소2"));

        // when
        addressService.delete(userId, first.getId());

        // then
        var list = addressService.list(userId);
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().getId()).isEqualTo(second.getId());
        assertThat(list.getFirst().isDefault()).isTrue();
    }

    @Test
    @DisplayName("유일한 기본배송지는 삭제할 수 없다")
    void 유일한_기본배송지_삭제불가() {
        // given
        var only = addressService.create(userId, req("집", false, "주소1"));

        // when & then
        assertThatThrownBy(() -> addressService.delete(userId, only.getId()))
            .isInstanceOf(com.kt.common.exception.CustomException.class)
            .hasMessage(ErrorCode.CANNOT_DELETE_LAST_DEFAULT_ADDRESS.getMessage());
    }

    @Test
    @DisplayName("배송지는 최대 10개까지 등록 가능하고 11번째는 실패한다")
    void 배송지_최대10개_제한() {
        // given
        for (int i = 1; i <= 10; i++) {
            addressService.create(userId, req("배송지" + i, false, "주소" + i));
        }
        assertThat(addressService.list(userId)).hasSize(10);

        // when & then
        assertThatThrownBy(() -> addressService.create(userId, req("배송지11", false, "주소11")))
            .isInstanceOf(com.kt.common.exception.CustomException.class)
            .hasMessage(ErrorCode.ADDRESS_LIMIT_EXCEEDED.getMessage());
    }

    private AddressRequest req(String alias, boolean isDefault, String address) {
        return new AddressRequest(
            alias,
            "홍길동",
            "010-1111-2222",
            address,
            "101동 203호",
            "12345",
            isDefault
        );
    }
}