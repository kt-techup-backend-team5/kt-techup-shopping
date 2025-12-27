package com.kt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.question.Answer;
import com.kt.domain.question.Question;
import com.kt.domain.user.User;
import com.kt.dto.question.AnswerRequest;
import com.kt.dto.question.AnswerResponse;
import com.kt.repository.question.AnswerRepository;
import com.kt.repository.question.QuestionRepository;
import com.kt.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AnswerService {

	private final AnswerRepository answerRepository;
	private final QuestionRepository questionRepository;
	private final UserRepository userRepository;

	// 답변 작성 (관리자)
	public void createAnswer(Long adminId, AnswerRequest.Create request) {
		User admin = userRepository.findByIdOrThrow(adminId);
		Question question = questionRepository.findByIdOrThrow(request.questionId());

		// 이미 답변이 있는지 확인
		Preconditions.validate(question.getAnswer() == null, ErrorCode.ALREADY_ANSWERED);

		Answer answer = new Answer(
				request.content(),
				admin,
				question
		);

		answerRepository.save(answer);
	}

	// 답변 조회 (문의 ID로)
	@Transactional(readOnly = true)
	public AnswerResponse getAnswerByQuestionId(Long questionId) {
		questionRepository.findByIdOrThrow(questionId); // 문의 존재 확인

		return answerRepository.findByQuestionId(questionId)
				.map(AnswerResponse::from)
				.orElse(null);
	}

	// 답변 수정 (관리자)
	public void updateAnswer(Long answerId, Long adminId, AnswerRequest.Update request) {
		Answer answer = findAnswerByIdAndValidateWriter(answerId, adminId, ErrorCode.NO_AUTHORITY_TO_UPDATE_ANSWER);
		answer.updateContent(request.content());
	}

	// 답변 삭제 (관리자)
	public void deleteAnswer(Long answerId, Long adminId) {
		Answer answer = findAnswerByIdAndValidateWriter(answerId, adminId, ErrorCode.NO_AUTHORITY_TO_DELETE_ANSWER);

		// Question의 상태를 다시 PENDING으로 변경
		answer.getQuestion().markAsPending();

		answerRepository.delete(answer);
	}

	// 답변 조회 및 작성자 검증 (헬퍼 메서드)
	private Answer findAnswerByIdAndValidateWriter(Long answerId, Long adminId, ErrorCode errorCode) {
		Answer answer = answerRepository.findByIdOrThrow(answerId);
		Preconditions.validate(answer.isWrittenBy(adminId), errorCode);
		return answer;
	}
}
