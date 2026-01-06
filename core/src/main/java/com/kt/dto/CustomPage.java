package com.kt.dto;

import java.util.List;

import com.kt.domain.user.User;

// 프론트엔드에서 페이징을 구현할 때 필요한 정보
// 데이터들
// 한화면에 몇개 보여줄것인가? => limit
// 내가 몇번째 페이지를 보고있나? => 전달받은거그대로 보내주기
// 몇개의 페이지가 생기나?(백엔드가 보내주면 좋아하는 영역)
// 한화면에 몇개를 보여주나 / 총 갯수
// 10개씩 보여주는데 데이터는 21개 => 3페이지
// 총 몇개의 데이터가 있나? (무조건) => count
public record CustomPage(
	List<User> users,
	int size,
	int page,
	int pages,
	long totalElements
) {

}
