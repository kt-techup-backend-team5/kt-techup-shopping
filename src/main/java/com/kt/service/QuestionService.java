package com.kt.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.product.Product;
import com.kt.domain.question.Question;
import com.kt.domain.user.User;
import com.kt.dto.question.QuestionRequest;
import com.kt.dto.question.QuestionResponse;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.question.QuestionRepository;
import com.kt.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class QuestionService {

	private final QuestionRepository questionRepository;
	private final UserRepository userRepository;
	private final ProductRepository productRepository;

	// 문의 작성
	public void createQuestion(Long userId, QuestionRequest.Create request) {
		User user = userRepository.findByIdOrThrow(userId);
		Product product = productRepository.findByIdOrThrow(request.productId());

		Question question = new Question(
				request.content(),
				request.isPublic(),
				user,
				product
		);

		Question savedQuestion = questionRepository.save(question);

		log.info("문의 작성 - questionId: {}, userId: {}, productId: {}, isPublic: {}",
			savedQuestion.getId(), userId, request.productId(), request.isPublic());
	}

	// 특정 상품의 문의 목록 조회 (공개 문의만)

	@Transactional(readOnly = true)
	public Page<QuestionResponse> getQuestionsByProductId(Long productId, Pageable pageable) {
		productRepository.findByIdOrThrow(productId); // 상품 존재 확인

		Page<Question> questions = questionRepository.findByProductId(productId, pageable);
		// TODO: 공개 문의만 필터링 로직 추가 필요 (QueryDSL 사용)
		return questions.map(QuestionResponse::from);
	}

	// 내 문의 목록 조회
	@Transactional(readOnly = true)
	public Page<QuestionResponse> getMyQuestions(Long userId, Pageable pageable) {
		Page<Question> questions = questionRepository.findByUserId(userId, pageable);
		return questions.map(QuestionResponse::from);
	}

	// 문의 상세 조회
	@Transactional(readOnly = true)
	public QuestionResponse getQuestionById(Long questionId, Long userId) {
		Question question = questionRepository.findByIdOrThrow(questionId);

		// 비공개 문의는 작성자만 조회 가능
		if (!question.isPublic()) {
			Preconditions.validate(question.isOwnedBy(userId), ErrorCode.NO_AUTHORITY_TO_UPDATE_QUESTION);
		}

		return QuestionResponse.from(question);
	}

	// 문의 내용 수정
	public void updateQuestion(Long questionId, Long userId, QuestionRequest.Update request) {
		Question question = findQuestionByIdAndValidateOwner(questionId, userId,
				ErrorCode.NO_AUTHORITY_TO_UPDATE_QUESTION);
		question.updateContent(request.content());
	}

	// 문의 공개 여부 수정
	public void updateQuestionPublicStatus(Long questionId, Long userId, QuestionRequest.UpdatePublic request) {
		Question question = findQuestionByIdAndValidateOwner(questionId, userId,
				ErrorCode.NO_AUTHORITY_TO_UPDATE_QUESTION);
		question.updateIsPublic(request.isPublic());
	}

	// 문의 삭제
	public void deleteQuestion(Long questionId, Long userId) {
		Question question = findQuestionByIdAndValidateOwner(questionId, userId,
				ErrorCode.NO_AUTHORITY_TO_DELETE_QUESTION);

		// 답변이 달린 문의는 삭제 불가
		Preconditions.validate(question.canDelete(), ErrorCode.CANNOT_DELETE_ANSWERED_QUESTION);

		questionRepository.delete(question);
	}

	// 관리자 - 전체 문의 목록 조회
	@Transactional(readOnly = true)
	public Page<QuestionResponse> getAdminQuestions(Pageable pageable) {
		Page<Question> questions = questionRepository.findAll(pageable);
		return questions.map(QuestionResponse::from);
	}

	// 관리자 - 문의 삭제 (강제 삭제, 답변 여부 무관)
	public void deleteQuestionByAdmin(Long questionId) {
		Question question = questionRepository.findByIdOrThrow(questionId);
		questionRepository.delete(question);
	}

	// 문의 조회 및 작성자 검증 (헬퍼 메서드)
	private Question findQuestionByIdAndValidateOwner(Long questionId, Long userId, ErrorCode errorCode) {
		Question question = questionRepository.findByIdOrThrow(questionId);
		Preconditions.validate(question.isOwnedBy(userId), errorCode);
		return question;
	}
}
