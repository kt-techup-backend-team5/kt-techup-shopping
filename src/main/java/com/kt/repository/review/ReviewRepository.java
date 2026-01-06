package com.kt.repository.review;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.product.Product;
import com.kt.domain.review.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

	default Review findByIdOrThrow(Long id) {
		return findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REVIEW));
	}

	Page<Review> findByProduct(Product product, Pageable pageable);

	boolean existsByOrderProductId(Long orderProductId);

    @Query("""
		select r
		from Review r
		where r.product.id = :productId
		  and r.isBlinded = false
		  and r.content is not null
		  and length(trim(r.content)) > 0
		order by r.id desc
	""")
    List<Review> findRecentForSummary(@Param("productId") Long productId, Pageable pageable);
}
