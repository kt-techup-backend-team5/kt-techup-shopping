package com.kt.repository.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.question.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {

	default Question findByIdOrThrow(Long id) {
		return findById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_QUESTION));
	}

	/**
	 * 특정 상품의 문의 목록 조회 (페이징)
	 */
	Page<Question> findByProductId(Long productId, Pageable pageable);

	/**
	 * 특정 사용자의 문의 목록 조회 (페이징)
	 */
	Page<Question> findByUserId(Long userId, Pageable pageable);
}
