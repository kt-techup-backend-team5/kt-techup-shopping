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
import com.kt.domain.user.Role;
import com.kt.domain.user.User;
import com.kt.dto.question.AnswerRequest;
import com.kt.dto.question.QuestionResponse;
import com.kt.repository.user.UserRepository;
import com.kt.security.JwtFilter;
import com.kt.security.JwtService;
import com.kt.security.WithMockCustomUser;
import com.kt.service.AnswerService;
import com.kt.service.QuestionService;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.UserFixture;

import org.springframework.context.annotation.Import;

@WebMvcTest(controllers = AdminQuestionController.class)
@WithMockCustomUser(id = 1L, role = Role.ADMIN)
@Import(SecurityConfiguration.class)
class AdminQuestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private QuestionService questionService;

	@MockitoBean
	private AnswerService answerService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private JwtFilter jwtFilter;

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
	@DisplayName("GET /admin/questions - 관리자 문의 목록 조회")
	void 관리자_문의_목록_조회() throws Exception {
		// given
		List<QuestionResponse> content = List.of(QuestionResponse.from(testQuestion));
		Page<QuestionResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 1);

		given(questionService.getAdminQuestions(any(Pageable.class))).willReturn(mockPage);

		// when
		ResultActions resultActions = mockMvc.perform(get("/admin/questions")
			.param("page", "0")
			.param("size", "10")
			.contentType(MediaType.APPLICATION_JSON))
			.andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].content").value(testQuestion.getContent()))
			.andExpect(jsonPath("$.data.content.length()").value(1));
		verify(questionService, times(1)).getAdminQuestions(any(Pageable.class));
	}

	@Test
	@DisplayName("DELETE /admin/questions/{questionId} - 관리자 문의 삭제")
	void 관리자_문의_삭제() throws Exception {
		// given
		Long questionId = 1L;

		willDoNothing().given(questionService).deleteQuestionByAdmin(anyLong());

		// when
		ResultActions resultActions = mockMvc.perform(delete("/admin/questions/{questionId}", questionId)
			.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"));
		verify(questionService, times(1)).deleteQuestionByAdmin(anyLong());
	}

	@Test
	@DisplayName("POST /admin/questions/answers - 답변 작성")
	void 답변_작성() throws Exception {
		// given
		AnswerRequest.Create request = new AnswerRequest.Create(1L, "답변 내용입니다.");

		willDoNothing().given(answerService).createAnswer(anyLong(), any(AnswerRequest.Create.class));

		// when
		ResultActions resultActions = mockMvc.perform(post("/admin/questions/answers")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"));
		verify(answerService, times(1)).createAnswer(anyLong(), any(AnswerRequest.Create.class));
	}

	@Test
	@DisplayName("PUT /admin/questions/answers/{answerId} - 답변 수정")
	void 답변_수정() throws Exception {
		// given
		Long answerId = 1L;
		AnswerRequest.Update request = new AnswerRequest.Update("수정된 답변 내용");

		willDoNothing().given(answerService).updateAnswer(anyLong(), anyLong(), any(AnswerRequest.Update.class));

		// when
		ResultActions resultActions = mockMvc.perform(put("/admin/questions/answers/{answerId}", answerId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"));
		verify(answerService, times(1)).updateAnswer(anyLong(), anyLong(), any(AnswerRequest.Update.class));
	}

	@Test
	@DisplayName("DELETE /admin/questions/answers/{answerId} - 답변 삭제")
	void 답변_삭제() throws Exception {
		// given
		Long answerId = 1L;

		willDoNothing().given(answerService).deleteAnswer(anyLong(), anyLong());

		// when
		ResultActions resultActions = mockMvc.perform(delete("/admin/questions/answers/{answerId}", answerId)
			.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"));
		verify(answerService, times(1)).deleteAnswer(anyLong(), anyLong());
	}
}
