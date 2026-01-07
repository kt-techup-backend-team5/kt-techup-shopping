package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.kt.domain.product.Product;
import com.kt.domain.review.Review;
import com.kt.domain.reviewsummary.ReviewSummary;
import com.kt.domain.user.User;
import com.kt.dto.reviewsummary.ReviewSummaryResponse;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.review.ReviewRepository;
import com.kt.repository.reviewsummary.ReviewSummaryRepository;
import com.kt.repository.user.UserRepository;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.UserFixture;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "review-summary.max-reviews=30",
    "review-summary.ttl-hours=24",
    "openai.prompt-version=test-v1"
})
class ReviewSummaryServiceTest {
    @Autowired
    private ReviewSummaryService reviewSummaryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewSummaryRepository reviewSummaryRepository;

    @MockBean
    private ReviewSummaryGenerator reviewSummaryGenerator;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = userRepository.save(UserFixture.defaultCustomer());
        product = productRepository.save(ProductFixture.product("원두A", 15000L, 100L, "원두 설명"));

        // AI 결과는 항상 고정값 반환(외부 호출 X)
        when(reviewSummaryGenerator.generate(any(), anyDouble(), any()))
            .thenReturn(new ReviewSummaryGenerator.Result(
                "테스트 요약입니다.",
                List.of("향이 좋음", "밸런스 좋음", "데일리로 무난"),
                List.of("개인 취향 차이", "향이 약하게 느껴질 수 있음", "진한 맛 선호자는 아쉬움"),
                List.of("밸런스", "향", "데일리"),
                "gpt-test",
                "test-v1"
            ));
    }

    @Test
    @DisplayName("캐시가 없으면 리뷰를 기반으로 요약을 생성하고 캐시에 저장한다")
    void 캐시없음_생성_저장() {
        // given
        reviewRepository.save(new Review("향이 깔끔하고 무난해요", 5, user, product, null));
        reviewRepository.save(new Review("라떼로 마시기 좋아요", 4, user, product, null));

        // when
        ReviewSummaryResponse response = reviewSummaryService.getOrGenerate(product.getId());

        // then
        assertThat(response.summary()).isEqualTo("테스트 요약입니다.");
        assertThat(reviewSummaryRepository.findByProductId(product.getId())).isPresent();

        // generator 1회 호출
        verify(reviewSummaryGenerator, times(1)).generate(any(), anyDouble(), any());
    }

    @Test
    @DisplayName("캐시가 유효하고 최신 리뷰 id가 동일하면 AI를 재호출하지 않고 캐시를 반환한다")
    void 캐시히트_재호출없음() {
        // given: 리뷰 1개
        Review r1 = reviewRepository.save(new Review("배송 빠르고 신선해요", 5, user, product, null));

        // 캐시를 미리 만들어 둠 (lastReviewIdUsed = r1.id, expiresAt = 미래)
        LocalDateTime now = LocalDateTime.now();
        ReviewSummary cached = ReviewSummary.of(
            product.getId(),
            1,
            r1.getId(),
            "기존 캐시 요약",
            List.of("장점1", "장점2", "장점3"),
            List.of("단점1", "단점2", "단점3"),
            List.of("키워드1", "키워드2", "키워드3"),
            "cached-model",
            "cached-v1",
            now,
            now.plusHours(24)
        );
        reviewSummaryRepository.save(cached);

        // when
        ReviewSummaryResponse response = reviewSummaryService.getOrGenerate(product.getId());

        // then: 캐시 그대로 반환
        assertThat(response.summary()).isEqualTo("기존 캐시 요약");

        // generator 호출 없음
        verify(reviewSummaryGenerator, never()).generate(any(), anyDouble(), any());
    }

    @Test
    @DisplayName("새 리뷰가 추가되어 최신 리뷰 id가 변경되면 캐시를 재생성한다")
    void 최신리뷰변경_재생성() {
        // given: 기존 리뷰 + 기존 캐시
        Review oldReview = reviewRepository.save(new Review("무난합니다", 4, user, product, null));

        LocalDateTime now = LocalDateTime.now();
        ReviewSummary cached = ReviewSummary.of(
            product.getId(),
            1,
            oldReview.getId(),
            "기존 캐시 요약",
            List.of("장점1", "장점2", "장점3"),
            List.of("단점1", "단점2", "단점3"),
            List.of("키워드1", "키워드2", "키워드3"),
            "cached-model",
            "cached-v1",
            now,
            now.plusHours(24)
        );
        reviewSummaryRepository.save(cached);

        // 새 리뷰 추가(최신 id 변경)
        reviewRepository.save(new Review("향이 더 좋아요", 5, user, product, null));

        // when
        ReviewSummaryResponse response = reviewSummaryService.getOrGenerate(product.getId());

        // then: mock 결과(재생성 결과) 반환
        assertThat(response.summary()).isEqualTo("테스트 요약입니다.");

        // generator 1회 호출
        verify(reviewSummaryGenerator, times(1)).generate(any(), anyDouble(), any());

        // 캐시가 갱신되었는지 확인(요약 내용 기준)
        var saved = reviewSummaryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(saved.getSummary()).isEqualTo("테스트 요약입니다.");
    }
}