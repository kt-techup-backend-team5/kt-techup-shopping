package com.kt.repository.question;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.question.Answer;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

	default Answer findByIdOrThrow(Long id) {
		return findById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ANSWER));
	}

	/**
	 * 특정 문의의 답변 조회
	 */
	Optional<Answer> findByQuestionId(Long questionId);
}
