package com.kt.support.fixture;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kt.domain.user.Gender;
import com.kt.domain.user.Role;
import com.kt.domain.user.User;

public final class UserFixture {
	private UserFixture() {}

	public static User defaultCustomer(){
		return new User(
				"test_user",
				"Password1234!",
				"테스트 사용자",
				"test@test.com",
				"010-1234-5678",
				Gender.MALE,
				LocalDate.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				Role.CUSTOMER
		);
	}

	public static User customer(String loginId){
		User user = defaultCustomer();
		// loginId만 바꾸고 싶으면 생성자 대신 customerWith(...) 방식으로 확장하는 걸 추천합니다.
		return new User(
				loginId,
				"Password1234!",
				"테스트 사용자2",
				"test@test.com",
				"010-1234-5678",
				Gender.MALE,
				LocalDate.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				Role.CUSTOMER
		);
	}
}
