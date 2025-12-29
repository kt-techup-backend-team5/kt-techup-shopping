package com.kt.controller.question;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.config.SecurityConfiguration;
import com.kt.domain.product.Product;
import com.kt.domain.question.Question;
import com.kt.domain.user.User;
import com.kt.dto.question.QuestionRequest;
import com.kt.dto.question.QuestionResponse;
import com.kt.repository.user.UserRepository;
import com.kt.security.JwtService;
import com.kt.security.WithMockCustomUser;
import com.kt.service.QuestionService;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.UserFixture;

import org.springframework.context.annotation.Import;

@WebMvcTest(controllers = QuestionController.class)
@WithMockCustomUser(id = 1L)
@Import(SecurityConfiguration.class)
class QuestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private QuestionService questionService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	private Question testQuestion;

	@BeforeEach
	void setUp() {
		User testUser = UserFixture.defaultCustomer();
		Product testProduct = ProductFixture.defaultProduct();
		testQuestion = new Question("테스트 문의 내용입니다.", true, testUser, testProduct);
	}

	@Test
	@DisplayName("POST /questions - 문의 작성")
	void 문의_작성() throws Exception {
		// given
		QuestionRequest.Create request = new QuestionRequest.Create(1L, "문의 내용입니다.", true);

		willDoNothing().given(questionService).createQuestion(anyLong(), any(QuestionRequest.Create.class));

		// when
		ResultActions resultActions = mockMvc.perform(post("/questions")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"));
		verify(questionService, times(1)).createQuestion(anyLong(), any(QuestionRequest.Create.class));
	}

	@Test
	@DisplayName("GET /questions - 상품별 문의 목록 조회")
	void 상품별_문의_목록_조회() throws Exception {
		// given
		Long productId = 1L;
		List<QuestionResponse> content = List.of(QuestionResponse.from(testQuestion));
		Page<QuestionResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 1);

		given(questionService.getQuestionsByProductId(eq(productId), any(Pageable.class))).willReturn(mockPage);

		// when
		ResultActions resultActions = mockMvc.perform(get("/questions")
			.param("productId", productId.toString())
			.param("page", "0")
			.param("size", "10")
			.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].content").value(testQuestion.getContent()))
			.andExpect(jsonPath("$.data.content.length()").value(1));
		verify(questionService, times(1)).getQuestionsByProductId(eq(productId), any(Pageable.class));
	}

	@Test
	@DisplayName("GET /questions/my - 내 문의 목록 조회")
	void 내_문의_목록_조회() throws Exception {
		// given
		List<QuestionResponse> content = List.of(QuestionResponse.from(testQuestion));
		Page<QuestionResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 1);

		given(questionService.getMyQuestions(anyLong(), any(Pageable.class))).willReturn(mockPage);

		// when
		ResultActions resultActions = mockMvc.perform(get("/questions/my")
			.param("page", "0")
			.param("size", "10")
			.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].content").value(testQuestion.getContent()))
			.andExpect(jsonPath("$.data.content.length()").value(1));
		verify(questionService, times(1)).getMyQuestions(anyLong(), any(Pageable.class));
	}

	@Test
	@DisplayName("GET /questions/{questionId} - 문의 상세 조회")
	void 문의_상세_조회() throws Exception {
		// given
		Long questionId = 1L;
		QuestionResponse response = QuestionResponse.from(testQuestion);

		given(questionService.getQuestionById(anyLong(), anyLong())).willReturn(response);

		// when
		ResultActions resultActions = mockMvc.perform(get("/questions/{questionId}", questionId)
			.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").value(testQuestion.getContent()));
		verify(questionService, times(1)).getQuestionById(anyLong(), anyLong());
	}

	@Test
	@DisplayName("PUT /questions/{questionId} - 문의 내용 수정")
	void 문의_내용_수정() throws Exception {
		// given
		Long questionId = 1L;
		QuestionRequest.Update request = new QuestionRequest.Update("수정된 문의 내용");

		willDoNothing().given(questionService).updateQuestion(anyLong(), anyLong(), any(QuestionRequest.Update.class));

		// when
		ResultActions resultActions = mockMvc.perform(put("/questions/{questionId}", questionId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"));
		verify(questionService, times(1)).updateQuestion(anyLong(), anyLong(), any(QuestionRequest.Update.class));
	}

	@Test
	@DisplayName("PATCH /questions/{questionId}/public - 문의 공개 여부 수정")
	void 문의_공개_여부_수정() throws Exception {
		// given
		Long questionId = 1L;
		QuestionRequest.UpdatePublic request = new QuestionRequest.UpdatePublic(false);

		willDoNothing().given(questionService).updateQuestionPublicStatus(anyLong(), anyLong(),
			any(QuestionRequest.UpdatePublic.class));

		// when
		ResultActions resultActions = mockMvc.perform(patch("/questions/{questionId}/public", questionId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"));
		verify(questionService, times(1)).updateQuestionPublicStatus(anyLong(), anyLong(),
			any(QuestionRequest.UpdatePublic.class));
	}

	@Test
	@DisplayName("DELETE /questions/{questionId} - 문의 삭제")
	void 문의_삭제() throws Exception {
		// given
		Long questionId = 1L;

		willDoNothing().given(questionService).deleteQuestion(anyLong(), anyLong());

		// when
		ResultActions resultActions = mockMvc.perform(delete("/questions/{questionId}", questionId)
			.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"));
		verify(questionService, times(1)).deleteQuestion(anyLong(), anyLong());
	}
}
