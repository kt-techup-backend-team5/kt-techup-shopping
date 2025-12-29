package com.kt.controller.payment;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.config.SecurityConfiguration;
import com.kt.domain.payment.PaymentType;
import com.kt.dto.payment.PaymentRequest;
import com.kt.repository.payment.PaymentTypeRepository;
import com.kt.repository.user.UserRepository;
import com.kt.security.JwtFilter;
import com.kt.security.JwtService;
import com.kt.security.WithMockCustomUser;
import com.kt.service.PaymentService;

@WebMvcTest(controllers = PaymentController.class)
@WithMockCustomUser(id = 1L)
@Import(SecurityConfiguration.class)
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentService paymentService;

	@MockitoBean
	private PaymentTypeRepository paymentTypeRepository;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private JwtFilter jwtFilter;

	@MockitoBean
	private UserRepository userRepository;

	@Test
	@DisplayName("POST /orders/{orderId}/pay - 결제 성공")
	void 결제_성공() throws Exception {
		// given
		Long orderId = 1L;
		String typeCode = "CARD";
		PaymentRequest request = new PaymentRequest(typeCode);
		PaymentType paymentType = new PaymentType(typeCode, "카드", "신용카드/체크카드 결제");

		given(paymentTypeRepository.findByTypeCodeOrThrow(typeCode)).willReturn(paymentType);
		willDoNothing().given(paymentService).pay(orderId, paymentType);

		// when & then
		mockMvc.perform(post("/orders/{orderId}/pay", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"))
			.andExpect(jsonPath("$.message").value("성공"));

		verify(paymentTypeRepository, times(1)).findByTypeCodeOrThrow(typeCode);
		verify(paymentService, times(1)).pay(orderId, paymentType);
	}

	@Test
	@DisplayName("POST /orders/{orderId}/pay - 결제 타입을 찾을 수 없음")
	void 결제_실패_결제타입_없음() throws Exception {
		// given
		Long orderId = 1L;
		String typeCode = "INVALID";
		PaymentRequest request = new PaymentRequest(typeCode);

		given(paymentTypeRepository.findByTypeCodeOrThrow(typeCode))
			.willThrow(new CustomException(ErrorCode.NOT_FOUND_PAYMENT_TYPE));

		// when & then
		mockMvc.perform(post("/orders/{orderId}/pay", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound());

		verify(paymentTypeRepository, times(1)).findByTypeCodeOrThrow(typeCode);
		verify(paymentService, never()).pay(anyLong(), any());
	}

	@Test
	@DisplayName("POST /orders/{orderId}/pay - 이미 결제된 주문")
	void 결제_실패_이미_결제된_주문() throws Exception {
		// given
		Long orderId = 1L;
		String typeCode = "CARD";
		PaymentRequest request = new PaymentRequest(typeCode);
		PaymentType paymentType = new PaymentType(typeCode, "카드", "신용카드/체크카드 결제");

		given(paymentTypeRepository.findByTypeCodeOrThrow(typeCode)).willReturn(paymentType);
		willThrow(new CustomException(ErrorCode.ALREADY_PAID_ORDER))
			.given(paymentService).pay(orderId, paymentType);

		// when & then
		mockMvc.perform(post("/orders/{orderId}/pay", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(paymentTypeRepository, times(1)).findByTypeCodeOrThrow(typeCode);
		verify(paymentService, times(1)).pay(orderId, paymentType);
	}

	@Test
	@DisplayName("POST /orders/{orderId}/pay - 결제 타입 필드가 비어있음")
	void 결제_실패_결제타입_필드_빈값() throws Exception {
		// given
		Long orderId = 1L;
		PaymentRequest request = new PaymentRequest("");

		given(paymentTypeRepository.findByTypeCodeOrThrow(""))
			.willThrow(new CustomException(ErrorCode.NOT_FOUND_PAYMENT_TYPE));

		// when & then
		mockMvc.perform(post("/orders/{orderId}/pay", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound());

		verify(paymentTypeRepository, times(1)).findByTypeCodeOrThrow("");
		verify(paymentService, never()).pay(anyLong(), any());
	}

	@Test
	@DisplayName("POST /orders/{orderId}/pay - 여러 결제 타입으로 결제 가능")
	void 여러_결제타입으로_결제() throws Exception {
		// given
		Long orderId = 1L;

		// CARD 결제
		String cardCode = "CARD";
		PaymentRequest cardRequest = new PaymentRequest(cardCode);
		PaymentType cardType = new PaymentType(cardCode, "카드", "카드 결제");
		given(paymentTypeRepository.findByTypeCodeOrThrow(cardCode)).willReturn(cardType);

		mockMvc.perform(post("/orders/{orderId}/pay", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(cardRequest)))
			.andExpect(status().isOk());

		// CASH 결제
		String cashCode = "CASH";
		PaymentRequest cashRequest = new PaymentRequest(cashCode);
		PaymentType cashType = new PaymentType(cashCode, "현금", "현금 결제");
		given(paymentTypeRepository.findByTypeCodeOrThrow(cashCode)).willReturn(cashType);

		mockMvc.perform(post("/orders/{orderId}/pay", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(cashRequest)))
			.andExpect(status().isOk());

		// PAY 결제
		String payCode = "PAY";
		PaymentRequest payRequest = new PaymentRequest(payCode);
		PaymentType payType = new PaymentType(payCode, "간편결제", "간편결제");
		given(paymentTypeRepository.findByTypeCodeOrThrow(payCode)).willReturn(payType);

		mockMvc.perform(post("/orders/{orderId}/pay", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payRequest)))
			.andExpect(status().isOk());
	}
}