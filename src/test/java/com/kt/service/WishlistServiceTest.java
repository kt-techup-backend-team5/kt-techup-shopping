package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.product.Product;
import com.kt.domain.user.User;
import com.kt.domain.wishlist.Wishlist;
import com.kt.dto.wishlist.WishlistResponse;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;
import com.kt.repository.wishlist.WishlistRepository;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
class WishlistServiceTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User user;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        user = userRepository.save(UserFixture.defaultCustomer());
        productA = productRepository.save(ProductFixture.product("상품A", 10000L, 10L, "설명A"));
        productB = productRepository.save(ProductFixture.product("상품B", 20000L, 5L, "설명B"));
    }

    @Test
    @DisplayName("찜 목록에 상품을 정상적으로 추가한다")
    void 찜_등록_성공() {
        // when
        wishlistService.add(user.getId(), productA.getId());

        // then
        List<Wishlist> wishlists = wishlistRepository.findAll();
        assertThat(wishlists).hasSize(1);
        assertThat(wishlists.get(0).getProduct().getId()).isEqualTo(productA.getId());
        assertThat(wishlists.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("이미 찜한 상품을 다시 등록하면 예외가 발생한다")
    void 찜_중복_등록_실패() {
        // given
        wishlistService.add(user.getId(), productA.getId());

        // when & then
        assertThatThrownBy(() -> wishlistService.add(user.getId(), productA.getId()))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.ALREADY_WISHLISTED.getMessage());
    }

    @Test
    @DisplayName("판매 중지된 상품을 찜하려 하면 예외가 발생한다")
    void 판매중지_상품_찜_실패() {
        // given
        productA.soldOut();
        productRepository.save(productA);

        // when & then
        assertThatThrownBy(() -> wishlistService.add(user.getId(), productA.getId()))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.NOT_ON_SALE_PRODUCT.getMessage());
    }

    @Test
    @DisplayName("찜 목록을 조회하면 최신순으로 페이징되어 반환된다")
    void 찜_목록_조회_페이징() {
        // given
        wishlistService.add(user.getId(), productA.getId());
        wishlistRepository.flush(); // 시간차 반영
        wishlistService.add(user.getId(), productB.getId());

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<WishlistResponse.Item> result = wishlistService.getMyWishlist(user.getId(), pageRequest);

        // then
        assertThat(result.getContent()).hasSize(2);
        // 최신순 정렬 확인 (B 먼저)
        assertThat(result.getContent().get(0).productName()).isEqualTo(productB.getName());
        assertThat(result.getContent().get(1).productName()).isEqualTo(productA.getName());
    }

    @Test
    @DisplayName("찜 해제가 정상적으로 동작한다")
    void 찜_해제_성공() {
        // given
        wishlistService.add(user.getId(), productA.getId());

        // when
        wishlistService.delete(user.getId(), productA.getId());

        // then
        List<Wishlist> wishlists = wishlistRepository.findAll();
        assertThat(wishlists).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 찜을 삭제해도 예외가 발생하지 않는다 (멱등성)")
    void 없는_찜_삭제_성공() {
        // when & then (에러 없이 통과)
        wishlistService.delete(user.getId(), 9999L);
    }
}