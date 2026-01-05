package com.kt.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.product.Product;
import com.kt.domain.question.Question;
import com.kt.domain.question.QuestionStatus;
import com.kt.domain.user.User;
import com.kt.dto.question.QuestionRequest;
import com.kt.dto.question.QuestionResponse;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.question.QuestionRepository;
import com.kt.repository.user.UserRepository;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.UserFixture;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
public class QuestionServiceTest {

	@Mock
	private QuestionRepository questionRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private QuestionService questionService;

	private User user;
	private Product product;
	private Question publicQuestion;

	@BeforeEach
	void setUp() {
		user = UserFixture.defaultCustomer();
		User admin = UserFixture.defaultAdmin();
		product = ProductFixture.defaultProduct();
		publicQuestion = new Question("테스트 문의 내용입니다.", true, user, product);
	}

	@Test
	void 문의_작성() {
		// given
		Long userId = 1L;
		Long productId = 1L;
		QuestionRequest.Create request = new QuestionRequest.Create(productId, "문의 내용입니다.", true);

		given(userRepository.findByIdOrThrow(userId)).willReturn(user);
		given(productRepository.findByIdOrThrow(productId)).willReturn(product);

		ArgumentCaptor<Question> argumentCaptor = ArgumentCaptor.forClass(Question.class);

		// save 후 반환할 Question 객체 (ID 포함)
		Question savedQuestion = spy(new Question("문의 내용입니다.", true, user, product));
		willReturn(1L).given(savedQuestion).getId();
		given(questionRepository.save(any(Question.class))).willReturn(savedQuestion);

		// when
		questionService.createQuestion(userId, request);

		// then
		verify(questionRepository, times(1)).save(argumentCaptor.capture());
		Question question = argumentCaptor.getValue();
		assertThat(question.getContent()).isEqualTo("문의 내용입니다.");
		assertThat(question.isPublic()).isTrue();
		assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
		assertThat(question.getUser()).isEqualTo(user);
		assertThat(question.getProduct()).isEqualTo(product);
	}

	@Test
	void 특정_상품의_문의_목록_조회() {
		// given
		Long productId = 1L;
		Pageable pageable = PageRequest.of(0, 10);

		given(productRepository.findByIdOrThrow(productId)).willReturn(product);
		given(questionRepository.findByProductId(productId, pageable))
			.willReturn(new PageImpl<>(List.of(publicQuestion)));

		// when
		Page<QuestionResponse> result = questionService.getQuestionsByProductId(productId, pageable);

		// then
		verify(productRepository, times(1)).findByIdOrThrow(productId);
		verify(questionRepository, times(1)).findByProductId(productId, pageable);
		assertThat(result).isNotEmpty();
		assertThat(result.getContent()).hasSize(1);
	}

	@Test
	void 내_문의_목록_조회() {
		// given
		Long userId = 1L;
		Pageable pageable = PageRequest.of(0, 10);

		given(questionRepository.findByUserId(userId, pageable))
			.willReturn(new PageImpl<>(List.of(publicQuestion)));

		// when
		Page<QuestionResponse> result = questionService.getMyQuestions(userId, pageable);

		// then
		verify(questionRepository, times(1)).findByUserId(userId, pageable);
		assertThat(result).isNotEmpty();
		assertThat(result.getContent()).hasSize(1);
	}

	@Test
	void 공개_문의_상세_조회() {
		// given
		Long questionId = 1L;
		Long userId = 1L;

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(publicQuestion);

		// when
		QuestionResponse result = questionService.getQuestionById(questionId, userId);

		// then
		verify(questionRepository, times(1)).findByIdOrThrow(questionId);
		assertThat(result).isNotNull();
		assertThat(result.content()).isEqualTo(publicQuestion.getContent());
	}

	@Test
	void 비공개_문의_상세_조회_작성자가_아닌_경우_예외() {
		// given
		Long questionId = 1L;
		Long userId = 999L; // 다른 사용자
		Question question = spy(new Question("비공개 문의 내용입니다.", false, user, product));

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(question);
		willReturn(false).given(question).isOwnedBy(userId);

		// when & then
		assertThatThrownBy(() -> questionService.getQuestionById(questionId, userId))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NO_AUTHORITY_TO_UPDATE_QUESTION.getMessage());
	}

	@Test
	void 문의_내용_수정() {
		// given
		Long questionId = 1L;
		Long userId = 1L;
		Question question = spy(new Question("테스트 문의 내용입니다.", true, user, product));
		QuestionRequest.Update request = new QuestionRequest.Update("수정된 문의 내용");

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(question);
		willReturn(true).given(question).isOwnedBy(userId);

		// when
		questionService.updateQuestion(questionId, userId, request);

		// then
		verify(questionRepository, times(1)).findByIdOrThrow(questionId);
		assertThat(question.getContent()).isEqualTo("수정된 문의 내용");
	}

	@Test
	void 문의_공개_여부_수정() {
		// given
		Long questionId = 1L;
		Long userId = 1L;
		Question question = spy(new Question("테스트 문의 내용입니다.", true, user, product));
		QuestionRequest.UpdatePublic request = new QuestionRequest.UpdatePublic(false);

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(question);
		willReturn(true).given(question).isOwnedBy(userId);

		// when
		questionService.updateQuestionPublicStatus(questionId, userId, request);

		// then
		verify(questionRepository, times(1)).findByIdOrThrow(questionId);
		assertThat(question.isPublic()).isFalse();
	}

	@Test
	void 문의_삭제() {
		// given
		Long questionId = 1L;
		Long userId = 1L;
		Question question = spy(new Question("테스트 문의 내용입니다.", true, user, product));

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(question);
		willReturn(true).given(question).isOwnedBy(userId);

		// when
		questionService.deleteQuestion(questionId, userId);

		// then
		verify(questionRepository, times(1)).findByIdOrThrow(questionId);
		verify(questionRepository, times(1)).delete(question);
	}

	@Test
	void 답변_달린_문의_삭제_시_예외() {
		// given
		Long questionId = 1L;
		Long userId = 1L;
		Question question = spy(new Question("테스트 문의 내용입니다.", true, user, product));
		question.markAsAnswered(); // 답변 완료 상태로 변경

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(question);
		willReturn(true).given(question).isOwnedBy(userId);

		// when & then
		assertThatThrownBy(() -> questionService.deleteQuestion(questionId, userId))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.CANNOT_DELETE_ANSWERED_QUESTION.getMessage());
	}

	@Test
	void 관리자_전체_문의_목록_조회() {
		// given
		Pageable pageable = PageRequest.of(0, 10);

		given(questionRepository.findAll(pageable))
			.willReturn(new PageImpl<>(List.of(publicQuestion)));

		// when
		Page<QuestionResponse> result = questionService.getAdminQuestions(pageable);

		// then
		verify(questionRepository, times(1)).findAll(pageable);
		assertThat(result).isNotEmpty();
		assertThat(result.getContent()).hasSize(1);
	}

	@Test
	void 관리자_문의_강제_삭제() {
		// given
		Long questionId = 1L;
		Question question = new Question("테스트 문의 내용입니다.", true, user, product);
		question.markAsAnswered(); // 답변 완료 상태

		given(questionRepository.findByIdOrThrow(questionId)).willReturn(question);

		// when
		questionService.deleteQuestionByAdmin(questionId);

		// then
		verify(questionRepository, times(1)).findByIdOrThrow(questionId);
		verify(questionRepository, times(1)).delete(question);
	}
}
