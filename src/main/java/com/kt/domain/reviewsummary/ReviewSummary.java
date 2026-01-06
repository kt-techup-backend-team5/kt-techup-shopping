package com.kt.domain.reviewsummary;

import com.kt.common.support.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@Table(
    name = "review_summaries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_summaries_product_id", columnNames = "product_id")
    }
)
@NoArgsConstructor
public class ReviewSummary extends BaseEntity {
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int reviewCountUsed;

    private Long lastReviewIdUsed;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<String> pros;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<String> cons;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<String> keywords;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String promptVersion;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public boolean isValid(LocalDateTime now) {
        return expiresAt.isAfter(now);
    }

    public static ReviewSummary of(
        Long productId,
        int reviewCountUsed,
        Long lastReviewIdUsed,
        String summary,
        List<String> pros,
        List<String> cons,
        List<String> keywords,
        String model,
        String promptVersion,
        LocalDateTime now,
        LocalDateTime expiresAt
    ) {
        var e = new ReviewSummary();
        e.productId = productId;
        e.reviewCountUsed = reviewCountUsed;
        e.lastReviewIdUsed = lastReviewIdUsed;
        e.summary = summary;
        e.pros = pros;
        e.cons = cons;
        e.keywords = keywords;
        e.model = model;
        e.promptVersion = promptVersion;
        e.generatedAt = now;
        e.expiresAt = expiresAt;
        return e;
    }
}