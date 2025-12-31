package com.kt.controller.point;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.config.SecurityConfiguration;
import com.kt.domain.point.PointHistory;
import com.kt.domain.point.PointHistoryType;
import com.kt.domain.user.Role;
import com.kt.domain.user.User;
import com.kt.dto.point.PointRequest;
import com.kt.repository.user.UserRepository;
import com.kt.security.JwtService;
import com.kt.security.WithMockCustomUser;
import com.kt.service.PointService;
import com.kt.support.fixture.UserFixture;

@WebMvcTest(controllers = AdminPointController.class)
@WithMockCustomUser(id = 1L, role = Role.ADMIN)
@Import(SecurityConfiguration.class)
class AdminPointControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PointService pointService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	@Test
	@DisplayName("GET /admin/points/{userId} - 관리자 포인트 이력 조회 성공")
	void 관리자_포인트_이력_조회_성공() throws Exception {
		// given
		Long userId = 2L;
		User user = UserFixture.defaultCustomer();

		PointHistory history1 = PointHistory.create(
				user,
				PointHistoryType.CREDITED_ORDER,
				1500L,
				6500L,
				"30,000원 주문 구매 확정"
		);
		PointHistory history2 = PointHistory.create(
				user,
				PointHistoryType.USED,
				-1000L,
				5000L,
				"1번 주문에서 포인트 사용"
		);

		Page<PointHistory> historyPage = new PageImpl<>(
				List.of(history1, history2),
				PageRequest.of(0, 10),
				2
		);

		given(pointService.getPointHistoryForAdmin(
				eq(userId),
				any(PageRequest.class)
		)).willReturn(historyPage);

		// when & then
		mockMvc.perform(get("/admin/points/{userId}", userId)
				.param("page", "1")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(2));

		verify(pointService, times(1)).getPointHistoryForAdmin(
				eq(userId),
				any(PageRequest.class)
		);
	}

	@Test
	@DisplayName("POST /admin/points/{userId}/adjust - 포인트 수동 조정 성공 (증가)")
	void 포인트_수동_증가_성공() throws Exception {
		// given
		Long userId = 2L;
		PointRequest.Adjust request = new PointRequest.Adjust(1000L, "관리자 포인트 지급");

		willDoNothing().given(pointService).adjustPoints(userId, 1000L, "관리자 포인트 지급");

		// when & then
		mockMvc.perform(post("/admin/points/{userId}/adjust", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"))
			.andExpect(jsonPath("$.message").value("성공"));

		verify(pointService, times(1)).adjustPoints(userId, 1000L, "관리자 포인트 지급");
	}

	@Test
	@DisplayName("POST /admin/points/{userId}/adjust - 포인트 수동 조정 성공 (감소)")
	void 포인트_수동_감소_성공() throws Exception {
		// given
		Long userId = 2L;
		PointRequest.Adjust request = new PointRequest.Adjust(-500L, "부적절한 적립 회수");

		willDoNothing().given(pointService).adjustPoints(userId, -500L, "부적절한 적립 회수");

		// when & then
		mockMvc.perform(post("/admin/points/{userId}/adjust", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"))
			.andExpect(jsonPath("$.message").value("성공"));

		verify(pointService, times(1)).adjustPoints(userId, -500L, "부적절한 적립 회수");
	}

	@Test
	@DisplayName("POST /admin/points/{userId}/adjust - 검증 실패 (amount null)")
	void 포인트_조정_실패_amount_null() throws Exception {
		// given
		Long userId = 2L;
		String requestBody = "{\"description\":\"테스트\"}";  // amount 누락

		// when & then
		mockMvc.perform(post("/admin/points/{userId}/adjust", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());

		verify(pointService, never()).adjustPoints(anyLong(), anyLong(), anyString());
	}

	@Test
	@DisplayName("POST /admin/points/{userId}/adjust - 검증 실패 (description blank)")
	void 포인트_조정_실패_description_blank() throws Exception {
		// given
		Long userId = 2L;
		PointRequest.Adjust request = new PointRequest.Adjust(1000L, "");

		// when & then
		mockMvc.perform(post("/admin/points/{userId}/adjust", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(pointService, never()).adjustPoints(anyLong(), anyLong(), anyString());
	}
}
