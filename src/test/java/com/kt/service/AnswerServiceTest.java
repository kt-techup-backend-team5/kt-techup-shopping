package com.kt.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.product.Product;
import com.kt.domain.question.Answer;
import com.kt.domain.question.Question;
import com.kt.domain.question.QuestionStatus;
import com.kt.domain.user.User;
import com.kt.dto.question.AnswerRequest;
import com.kt.dto.question.AnswerResponse;
import com.kt.repository.question.AnswerRepository;
import com.kt.repository.question.QuestionRepository;
import com.kt.repository.user.UserRepository;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.UserFixture;

import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
public class AnswerServiceTest {

	@Mock
	private AnswerRepository answerRepository;

	@Mock
	private QuestionRepository questionRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AnswerService answerService;

	private User testAdmin;
	private Question testQuestion;

	@BeforeEach
	void setUp() {
		testAdmin = UserFixture.defaultAdmin();
		User testCustomer = UserFixture.defaultCustomer();
		Product testProduct = ProductFixture.defaultProduct();
		testQuestion = new Question("테스트 문의 내용입니다.", true, testCustomer, testProduct);
	}

	@Test
	void 답변_작성() {
		// given
		Long adminId = 1L;
		Long questionId = 1L;
		AnswerRequest.Create request = new AnswerRequest.Create(questionId, "답변 내용입니다.");

		given(userRepository.findByIdOrThrow(adminId)).willReturn(testAdmin);
		given(questionRepository.findByIdOrThrow(questionId)).willReturn(testQuestion);

		ArgumentCaptor<Answer> argumentCaptor = ArgumentCaptor.forClass(Answer.class);

		// when
		answerService.createAnswer(adminId, request);

		// then
		verify(answerRepository, times(1)).save(argumentCaptor.capture());
		Answer answer = argumentCaptor.getValue();
		assertThat(answer.getContent()).isEqualTo("답변 내용입니다.");
		assertThat(answer.getAdmin()).isEqualTo(testAdmin);
		assertThat(answer.getQuestion()).isEqualTo(testQuestion);
		assertThat(testQuestion.getStatus()).isEqualTo(QuestionStatus.ANSWERED);
	}

	@Test
	void 이미_답변_존재하는_경우_예외() {
		// given
		Long adminId = 1L;
		Long questionId = 1L;
		new Answer("기존 답변", testAdmin, testQuestion); // question을 ANSWERED 상태로 변경
		AnswerRequest.Create request = new AnswerRequest.Create(questionId, "새 답변 내용");

		given(userRepository.findByIdOrThrow(adminId)).willReturn(testAdmin);
		given(questionRepository.findByIdOrThrow(questionId)).willReturn(testQuestion);

		// when & then
		assertThatThrownBy(() -> answerService.createAnswer(adminId, request))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.ALREADY_ANSWERED.getMessage());
	}

	@Test
	void 문의_ID로_답변_조회() {
		// given
		Long questionId = 1L;
		Answer answer = new Answer("테스트 답변 내용입니다.", testAdmin, testQuestion);

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(testQuestion);
		given(answerRepository.findByQuestionId(questionId)).willReturn(Optional.of(answer));

		// when
		AnswerResponse result = answerService.getAnswerByQuestionId(questionId);

		// then
		verify(questionRepository, times(1)).findByIdOrThrow(questionId);
		verify(answerRepository, times(1)).findByQuestionId(questionId);
		assertThat(result).isNotNull();
		assertThat(result.content()).isEqualTo(answer.getContent());
	}

	@Test
	void 답변_없는_경우_null_반환() {
		// given
		Long questionId = 1L;

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(testQuestion);
		given(answerRepository.findByQuestionId(questionId)).willReturn(Optional.empty());

		// when
		AnswerResponse result = answerService.getAnswerByQuestionId(questionId);

		// then
		verify(questionRepository, times(1)).findByIdOrThrow(questionId);
		verify(answerRepository, times(1)).findByQuestionId(questionId);
		assertThat(result).isNull();
	}

	@Test
	void 답변_수정() {
		// given
		Long answerId = 1L;
		Long adminId = 1L;
		Answer answer = spy(new Answer("테스트 답변 내용입니다.", testAdmin, testQuestion));
		AnswerRequest.Update request = new AnswerRequest.Update("수정된 답변 내용");

		given(answerRepository.findByIdOrThrow(answerId)).willReturn(answer);
		willReturn(true).given(answer).isWrittenBy(adminId);

		// when
		answerService.updateAnswer(answerId, adminId, request);

		// then
		verify(answerRepository, times(1)).findByIdOrThrow(answerId);
		assertThat(answer.getContent()).isEqualTo("수정된 답변 내용");
	}

	@Test
	void 답변_수정_권한_없는_경우_예외() {
		// given
		Long answerId = 1L;
		Long otherAdminId = 999L; // 다른 관리자
		Answer answer = spy(new Answer("테스트 답변 내용입니다.", testAdmin, testQuestion));
		AnswerRequest.Update request = new AnswerRequest.Update("수정된 답변 내용");

		given(answerRepository.findByIdOrThrow(answerId)).willReturn(answer);
		willReturn(false).given(answer).isWrittenBy(otherAdminId);

		// when & then
		assertThatThrownBy(() -> answerService.updateAnswer(answerId, otherAdminId, request))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NO_AUTHORITY_TO_UPDATE_ANSWER.getMessage());
	}

	@Test
	void 답변_삭제() {
		// given
		Long answerId = 1L;
		Long adminId = 1L;
		Answer answer = spy(new Answer("테스트 답변 내용입니다.", testAdmin, testQuestion));

		given(answerRepository.findByIdOrThrow(answerId)).willReturn(answer);
		willReturn(true).given(answer).isWrittenBy(adminId);

		// when
		answerService.deleteAnswer(answerId, adminId);

		// then
		verify(answerRepository, times(1)).findByIdOrThrow(answerId);
		verify(answerRepository, times(1)).delete(answer);
		assertThat(testQuestion.getStatus()).isEqualTo(QuestionStatus.PENDING);
	}

	@Test
	void 답변_삭제_권한_없는_경우_예외() {
		// given
		Long answerId = 1L;
		Long otherAdminId = 999L; // 다른 관리자
		Answer answer = spy(new Answer("테스트 답변 내용입니다.", testAdmin, testQuestion));

		given(answerRepository.findByIdOrThrow(answerId)).willReturn(answer);
		willReturn(false).given(answer).isWrittenBy(otherAdminId);

		// when & then
		assertThatThrownBy(() -> answerService.deleteAnswer(answerId, otherAdminId))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NO_AUTHORITY_TO_DELETE_ANSWER.getMessage());
	}
}
