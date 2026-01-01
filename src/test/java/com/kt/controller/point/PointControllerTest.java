package com.kt.controller.point;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kt.config.SecurityConfiguration;
import com.kt.domain.point.PointHistory;
import com.kt.domain.point.PointHistoryType;
import com.kt.domain.user.User;
import com.kt.repository.user.UserRepository;
import com.kt.security.JwtService;
import com.kt.security.WithMockCustomUser;
import com.kt.service.PointService;
import com.kt.support.fixture.UserFixture;

@WebMvcTest(controllers = PointController.class)
@WithMockCustomUser(id = 1L)
@Import(SecurityConfiguration.class)
class PointControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PointService pointService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	@Test
	@DisplayName("GET /users/me/points - 포인트 잔액 조회 성공")
	void 포인트_잔액_조회_성공() throws Exception {
		// given
		Long userId = 1L;
		Long availablePoints = 5000L;

		given(pointService.getAvailablePoints(userId)).willReturn(availablePoints);

		// when & then
		mockMvc.perform(get("/users/me/points"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"))
			.andExpect(jsonPath("$.message").value("성공"))
			.andExpect(jsonPath("$.data.availablePoints").value(5000));

		verify(pointService, times(1)).getAvailablePoints(userId);
	}

	@Test
	@DisplayName("GET /users/me/points - 포인트가 없는 경우 0 반환")
	void 포인트_없는_경우() throws Exception {
		// given
		Long userId = 1L;

		given(pointService.getAvailablePoints(userId)).willReturn(0L);

		// when & then
		mockMvc.perform(get("/users/me/points"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"))
			.andExpect(jsonPath("$.data.availablePoints").value(0));

		verify(pointService, times(1)).getAvailablePoints(userId);
	}

	@Test
	@DisplayName("GET /users/me/points/history - 포인트 이력 조회 성공")
	void 포인트_이력_조회_성공() throws Exception {
		// given
		Long userId = 1L;
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

		given(pointService.getPointHistory(
				eq(userId),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(PageRequest.class)
		)).willReturn(historyPage);

		// when & then
		mockMvc.perform(get("/users/me/points/history")
				.param("page", "1")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(2));

		verify(pointService, times(1)).getPointHistory(
				eq(userId),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(PageRequest.class)
		);
	}

	@Test
	@DisplayName("GET /users/me/points/history - 기간 필터링 조회")
	void 포인트_이력_기간_필터링() throws Exception {
		// given
		Long userId = 1L;
		LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);

		Page<PointHistory> emptyPage = new PageImpl<>(
				List.of(),
				PageRequest.of(0, 10),
				0
		);

		given(pointService.getPointHistory(
				eq(userId),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(PageRequest.class)
		)).willReturn(emptyPage);

		// when & then
		mockMvc.perform(get("/users/me/points/history")
				.param("startDate", "2024-01-01T00:00:00")
				.param("endDate", "2024-12-31T23:59:59")
				.param("page", "1")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("ok"))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.totalElements").value(0));

		verify(pointService, times(1)).getPointHistory(
				eq(userId),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(PageRequest.class)
		);
	}
}
