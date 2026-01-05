package com.kt.repository.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {
	default Product findByIdOrThrow(Long id) {
		return findById(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PRODUCT));
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Product p WHERE p.id = :id")
	Optional<Product> findByIdPessimistic(Long id);

	@Query("SELECT p FROM Product p " +
			"WHERE (:keyword = '' OR p.name LIKE %:keyword%) " +
			"AND p.status IN :statuses")
	Page<Product> findAllByKeywordAndStatuses(
			@Param("keyword") String keyword,
			@Param("statuses") List<ProductStatus> statuses,
			Pageable pageable);

	@Query("SELECT p FROM Product p "
			+ "WHERE p.stock <= :threshold "
			+ "AND p.status IN :statuses")
	Page<Product> findAllByLowStock(
			@Param("threshold") Long threshold,
			@Param("statuses") List<ProductStatus> statuses,
			Pageable pageable);
}
