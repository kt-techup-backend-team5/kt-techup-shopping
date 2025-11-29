package com.kt.repository.review;

import com.kt.domain.product.Product;
import com.kt.domain.review.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	Page<Review> findByProduct(Product product, Pageable pageable);

	boolean existsByOrderProductId(Long orderProductId);

}
