package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.cart.CartItem;
import com.kt.domain.product.Product;
import com.kt.domain.user.User;
import com.kt.repository.cart.CartItemRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;
import com.kt.service.CartService.GuestCartItem;
import com.kt.service.CartService.MergeResult;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.UserFixture;
import java.util.List;
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
class CartServiceTest {
    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private User user;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        user = userRepository.save(UserFixture.defaultCustomer());

        // 상품 생성 (A: 재고 10개, B: 재고 5개)
        productA = productRepository.save(ProductFixture.product("상품A", 10000L, 10L, "설명A"));
        productB = productRepository.save(ProductFixture.product("상품B", 20000L, 5L, "설명B"));
    }

    @Test
    @DisplayName("장바구니에 새로운 상품을 추가한다")
    void 장바구니_상품_추가_신규() {
        // when
        CartItem item = cartService.add(user.getId(), productA.getId(), 2L);

        // then
        assertThat(item.getProduct().getId()).isEqualTo(productA.getId());
        assertThat(item.getQuantity()).isEqualTo(2L);
        assertThat(cartItemRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("이미 담긴 상품을 추가하면 수량이 합산된다")
    void 장바구니_상품_추가_기존_합산() {
        // given
        cartService.add(user.getId(), productA.getId(), 2L);

        // when
        CartItem item = cartService.add(user.getId(), productA.getId(), 3L);

        // then
        assertThat(item.getQuantity()).isEqualTo(5L); // 2 + 3
        assertThat(cartItemRepository.findAll()).hasSize(1); // Row는 1개 유지
    }

    @Test
    @DisplayName("재고보다 많은 수량을 담으려 하면 예외가 발생한다")
    void 장바구니_재고_초과_예외() {
        // given (재고 10개)
        Long requestQuantity = 11L;

        // when & then
        assertThatThrownBy(() -> cartService.add(user.getId(), productA.getId(), requestQuantity))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.NOT_ENOUGH_STOCK.getMessage());
    }

    @Test
    @DisplayName("판매 중지된 상품은 장바구니에 담을 수 없다")
    void 판매중지_상품_담기_불가() {
        // given
        productA.soldOut(); // 혹은 inActivate() 등 사용
        productRepository.save(productA);

        // when & then
        assertThatThrownBy(() -> cartService.add(user.getId(), productA.getId(), 1L))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.NOT_ON_SALE_PRODUCT.getMessage());
    }

    @Test
    @DisplayName("장바구니 수량을 변경한다 (덮어쓰기)")
    void 장바구니_수량_변경() {
        // given
        cartService.add(user.getId(), productA.getId(), 2L);

        // when
        CartItem updated = cartService.changeQuantity(user.getId(), productA.getId(), 5L);

        // then
        assertThat(updated.getQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("장바구니 상품을 삭제한다")
    void 장바구니_삭제() {
        // given
        cartService.add(user.getId(), productA.getId(), 1L);

        // when
        cartService.deleteItem(user.getId(), productA.getId());

        // then
        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("타인의 장바구니 상품을 삭제하려 하면 예외가 발생한다")
    void 타인_장바구니_삭제_불가() {
        // given
        User otherUser = userRepository.save(UserFixture.customer("other_user"));
        cartService.add(otherUser.getId(), productA.getId(), 1L);

        // when & then (내 아이디로 남의 상품 삭제 시도)
        assertThatThrownBy(() -> cartService.deleteItem(user.getId(), productA.getId()))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.NOT_FOUND_CART_ITEM.getMessage());
    }

    @Test
    @DisplayName("장바구니를 전체 비운다")
    void 장바구니_비우기() {
        // given
        cartService.add(user.getId(), productA.getId(), 1L);
        cartService.add(user.getId(), productB.getId(), 1L);

        // when
        cartService.clear(user.getId());

        // then
        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("장바구니 목록을 조회하면 최근 수정순으로 정렬된다")
    void 장바구니_목록_조회() throws InterruptedException {
        // given
        cartService.add(user.getId(), productA.getId(), 1L);
        Thread.sleep(10); // 정렬 테스트를 위한 시간차
        cartService.add(user.getId(), productB.getId(), 1L);

        // when
        List<CartItem> myCart = cartService.getMyCart(user.getId());

        // then
        assertThat(myCart).hasSize(2);
        assertThat(myCart.get(0).getProduct().getId()).isEqualTo(productB.getId());
        assertThat(myCart.get(1).getProduct().getId()).isEqualTo(productA.getId());
    }

    @Test
    @DisplayName("비회원 장바구니 병합 시 정상 상품은 합쳐지고, 재고 부족 상품은 수량 최대치로 조정된다")
    void 비회원_병합_성공() {
        // given
        // 1. 기존 회원 장바구니에 A 상품 2개 존재
        cartService.add(user.getId(), productA.getId(), 2L);

        // 2. 비회원 장바구니 (A: 3개 추가, B: 100개(재고초과) 추가)
        List<GuestCartItem> guestItems = List.of(
            new GuestCartItem(productA.getId(), 3L), // 정상 합산 예상
            new GuestCartItem(productB.getId(), 100L) // 재고 부족으로 최대치 조정 예상
        );

        // when
        MergeResult result = cartService.merge(user.getId(), guestItems);

        // then
        // 1. 장바구니 확인
        List<CartItem> cartItems = result.cartItems();
        assertThat(cartItems).hasSize(2);

        // A상품: 기존 2 + 비회원 3 = 5
        assertThat(cartItems.stream()
            .filter(i -> i.getProduct().getId().equals(productA.getId()))
            .findFirst().get().getQuantity()).isEqualTo(5L);

        // B상품: 재고 5개인데 100개 요청 -> 로직에 따라 5개만 담김 (isMergeMode = true 로직)
        assertThat(cartItems.stream()
            .filter(i -> i.getProduct().getId().equals(productB.getId()))
            .findFirst().get().getQuantity()).isEqualTo(5L); // productB 재고 MaxCap

        // 2. 제외 목록 확인
        // 현재 로직상 재고 부족은 merge 시 Math.min으로 처리되어 저장되므로 excluded에 들어가지 않음
        assertThat(result.excludedItems()).isEmpty();
    }

    @Test
    @DisplayName("비회원 병합 시 판매 중지된 상품은 제외 목록(excludedItems)에 포함된다")
    void 비회원_병합_판매중지_제외() {
        // given
        productB.soldOut();
        productRepository.save(productB);

        List<GuestCartItem> guestItems = List.of(
            new GuestCartItem(productA.getId(), 1L),
            new GuestCartItem(productB.getId(), 1L)
        );

        // when
        MergeResult result = cartService.merge(user.getId(), guestItems);

        // then
        // 장바구니에는 A만 있어야 함
        assertThat(result.cartItems()).hasSize(1);
        assertThat(result.cartItems().get(0).getProduct().getId()).isEqualTo(productA.getId());

        // 제외 목록에 B가 있어야 함
        assertThat(result.excludedItems()).hasSize(1);
        assertThat(result.excludedItems())
            .extracting("productId", "reason")
            .contains(tuple(productB.getId(), CartService.ExcludeReason.NOT_ON_SALE));
    }
}