package com.kt.common;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "검증 실패"),
	@ApiResponse(responseCode = "500", description = "서버 에러 - 백엔드에 바로 문의 바랍니다.")
})
public abstract class SwaggerAssistance {
}
