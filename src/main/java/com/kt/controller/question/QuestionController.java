package com.kt.controller.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.dto.question.QuestionRequest;
import com.kt.dto.question.QuestionResponse;
import com.kt.security.DefaultCurrentUser;
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

@Tag(name = "Questions", description = "상품 문의 API")
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class QuestionController {

	private final QuestionService questionService;

	@Operation(
		summary = "문의 작성",
		description = "상품에 대한 문의를 작성합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "작성 성공"),
		@ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
	})
	@PostMapping
	public ApiResult<Void> createQuestion(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@RequestBody @Valid QuestionRequest.Create request
	) {
		questionService.createQuestion(currentUser.getId(), request);
		return ApiResult.ok();
	}

	@Operation(
		summary = "상품별 문의 목록 조회",
		description = "특정 상품의 공개 문의 목록을 페이징하여 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
	})
	@Parameters({
		@Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
		@Parameter(name = "size", description = "페이지 당 항목 수", example = "10"),
		@Parameter(name = "sort", description = "정렬 기준 (예: 'createdAt,desc' (최신순))", example = "createdAt,desc")
	})
	@GetMapping
	public ApiResult<Page<QuestionResponse>> getQuestionsByProductId(
		@Parameter(description = "문의를 조회할 상품 ID", required = true)
		@RequestParam Long productId,
		Pageable pageable
	) {
		return ApiResult.ok(questionService.getQuestionsByProductId(productId, pageable));
	}

	@Operation(
		summary = "내 문의 목록 조회",
		description = "로그인한 사용자가 작성한 문의 목록을 페이징하여 조회합니다."
	)
	@Parameters({
		@Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
		@Parameter(name = "size", description = "페이지 당 항목 수", example = "10"),
		@Parameter(name = "sort", description = "정렬 기준 (예: 'createdAt,desc' (최신순))", example = "createdAt,desc")
	})
	@GetMapping("/my")
	public ApiResult<Page<QuestionResponse>> getMyQuestions(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		Pageable pageable
	) {
		return ApiResult.ok(questionService.getMyQuestions(currentUser.getId(), pageable));
	}

	@Operation(
		summary = "문의 상세 조회",
		description = "문의 상세 정보를 조회합니다. 비공개 문의는 작성자만 조회 가능합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "403", description = "문의를 조회할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
	})
	@GetMapping("/{questionId}")
	public ApiResult<QuestionResponse> getQuestionById(
		@Parameter(description = "조회할 문의 ID", required = true)
		@PathVariable Long questionId,
		@AuthenticationPrincipal DefaultCurrentUser currentUser
	) {
		return ApiResult.ok(questionService.getQuestionById(questionId, currentUser.getId()));
	}

	@Operation(
		summary = "문의 내용 수정",
		description = "작성자 본인이 작성한 문의 내용을 수정합니다. 답변이 달린 문의는 수정할 수 없습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "답변이 달린 문의는 수정할 수 없음"),
		@ApiResponse(responseCode = "403", description = "문의를 수정할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
	})
	@PutMapping("/{questionId}")
	public ApiResult<Void> updateQuestion(
		@Parameter(description = "수정할 문의 ID", required = true)
		@PathVariable Long questionId,
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@RequestBody @Valid QuestionRequest.Update request
	) {
		questionService.updateQuestion(questionId, currentUser.getId(), request);
		return ApiResult.ok();
	}

	@Operation(
		summary = "문의 공개 여부 수정",
		description = "작성자 본인이 작성한 문의의 공개 여부를 변경합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "403", description = "문의를 수정할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
	})
	@PatchMapping("/{questionId}/public")
	public ApiResult<Void> updateQuestionPublicStatus(
		@Parameter(description = "수정할 문의 ID", required = true)
		@PathVariable Long questionId,
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@RequestBody @Valid QuestionRequest.UpdatePublic request
	) {
		questionService.updateQuestionPublicStatus(questionId, currentUser.getId(), request);
		return ApiResult.ok();
	}

	@Operation(
		summary = "문의 삭제",
		description = "작성자 본인이 작성한 문의를 삭제합니다. 답변이 달린 문의는 삭제할 수 없습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "400", description = "답변이 달린 문의는 삭제할 수 없음"),
		@ApiResponse(responseCode = "403", description = "문의를 삭제할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
	})
	@DeleteMapping("/{questionId}")
	public ApiResult<Void> deleteQuestion(
		@Parameter(description = "삭제할 문의 ID", required = true)
		@PathVariable Long questionId,
		@AuthenticationPrincipal DefaultCurrentUser currentUser
	) {
		questionService.deleteQuestion(questionId, currentUser.getId());
		return ApiResult.ok();
	}
}
