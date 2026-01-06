package com.kt.repository.review;

import com.kt.domain.review.QReview;
import com.kt.domain.review.Review;
import com.kt.dto.review.ReviewSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ReviewRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Review> searchReviews(ReviewSearchCondition condition, Pageable pageable) {
        QReview review = QReview.review;

        List<Review> content = queryFactory
                .selectFrom(review)
                .where(
                        productNameContains(condition.getProductName()),
                        userNameContains(condition.getUserName()),
                        ratingEq(condition.getRating()),
                        isBlindedEq(condition.getIsBlinded()),
                        ratingGoe(condition.getMinRating()),
                        ratingLoe(condition.getMaxRating())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(review.count())
                .from(review)
                .where(
                        productNameContains(condition.getProductName()),
                        userNameContains(condition.getUserName()),
                        ratingEq(condition.getRating()),
                        isBlindedEq(condition.getIsBlinded()),
                        ratingGoe(condition.getMinRating()),
                        ratingLoe(condition.getMaxRating())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    private BooleanExpression productNameContains(String productName) {
        return StringUtils.hasText(productName) ? QReview.review.product.name.contains(productName) : null;
    }

    private BooleanExpression userNameContains(String userName) {
        return StringUtils.hasText(userName) ? QReview.review.user.name.contains(userName) : null;
    }

    private BooleanExpression ratingEq(Integer rating) {
        return rating != null ? QReview.review.rating.eq(rating) : null;
    }

    private BooleanExpression isBlindedEq(Boolean isBlinded) {
        return isBlinded != null ? QReview.review.isBlinded.eq(isBlinded) : null;
    }

    private BooleanExpression ratingGoe(Integer minRating) {
        return minRating != null ? QReview.review.rating.goe(minRating) : null;
    }

    private BooleanExpression ratingLoe(Integer maxRating) {
        return maxRating != null ? QReview.review.rating.loe(maxRating) : null;
    }
}
