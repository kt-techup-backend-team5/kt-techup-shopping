package com.kt.controller.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.question.AnswerRequest;
import com.kt.dto.question.QuestionResponse;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.AnswerService;
import com.kt.service.QuestionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Questions", description = "관리자 문의/답변 관련 API")
@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionController extends SwaggerAssistance {

	private final QuestionService questionService;
	private final AnswerService answerService;

	@Operation(
		summary = "관리자 문의 목록 조회",
		description = "관리자가 전체 문의 목록을 페이징하여 조회합니다."
	)
	@Parameters({
		@Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
		@Parameter(name = "size", description = "페이지 당 항목 수", example = "10"),
		@Parameter(name = "sort", description = "정렬 기준 (예: 'createdAt,desc' (최신순))", example = "createdAt,desc")
	})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 없음")
	})
	@GetMapping
	public ApiResult<Page<QuestionResponse>> getAdminQuestions(Pageable pageable) {
		return ApiResult.ok(questionService.getAdminQuestions(pageable));
	}

	@Operation(
		summary = "관리자 문의 삭제",
		description = "관리자가 문의를 삭제합니다. 답변 여부와 관계없이 삭제 가능합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 없음"),
		@ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
	})
	@DeleteMapping("/{questionId}")
	public ApiResult<Void> deleteQuestionByAdmin(
		@Parameter(description = "삭제할 문의 ID", required = true)
		@PathVariable Long questionId
	) {
		questionService.deleteQuestionByAdmin(questionId);
		return ApiResult.ok();
	}

	@Operation(
		summary = "답변 작성",
		description = "관리자가 문의에 대한 답변을 작성합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "작성 성공"),
		@ApiResponse(responseCode = "400", description = "이미 답변이 달린 문의"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 없음"),
		@ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
	})
	@PostMapping("/answers")
	public ApiResult<Void> createAnswer(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@RequestBody @Valid AnswerRequest.Create request
	) {
		answerService.createAnswer(currentUser.getId(), request);
		return ApiResult.ok();
	}

	@Operation(
		summary = "답변 수정",
		description = "관리자가 작성한 답변을 수정합니다. 답변 작성자만 수정 가능합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 없음 또는 답변 작성자가 아님"),
		@ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
	})
	@PutMapping("/answers/{answerId}")
	public ApiResult<Void> updateAnswer(
		@Parameter(description = "수정할 답변 ID", required = true)
		@PathVariable Long answerId,
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@RequestBody @Valid AnswerRequest.Update request
	) {
		answerService.updateAnswer(answerId, currentUser.getId(), request);
		return ApiResult.ok();
	}

	@Operation(
		summary = "답변 삭제",
		description = "관리자가 작성한 답변을 삭제합니다. 답변 작성자만 삭제 가능하며, 삭제 시 문의 상태가 답변 대기로 변경됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 없음 또는 답변 작성자가 아님"),
		@ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
	})
	@DeleteMapping("/answers/{answerId}")
	public ApiResult<Void> deleteAnswer(
		@Parameter(description = "삭제할 답변 ID", required = true)
		@PathVariable Long answerId,
		@AuthenticationPrincipal DefaultCurrentUser currentUser
	) {
		answerService.deleteAnswer(answerId, currentUser.getId());
		return ApiResult.ok();
	}
}
