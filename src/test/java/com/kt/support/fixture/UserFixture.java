package com.kt.support.fixture;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kt.domain.user.Gender;
import com.kt.domain.user.Role;
import com.kt.domain.user.User;

/**
 * User 엔티티의 테스트 데이터를 생성하는 Fixture 클래스
 * <p>
 * 테스트에서 User 객체가 필요할 때 일관된 테스트 데이터를 제공합니다.
 * 각 메서드는 독립적인 User 인스턴스를 생성하므로 테스트 간 격리가 보장됩니다.
 * </p>
 */
public final class UserFixture {
	private UserFixture() {}

	/**
	 * 기본 고객(Customer) 사용자를 생성합니다.
	 * <p>
	 * 로그인 ID: test_user<br>
	 * 이메일: customer1@test.com<br>
	 * 역할: CUSTOMER
	 * </p>
	 *
	 * @return 기본 설정값을 가진 고객 User 객체
	 */
	public static User defaultCustomer(){
		return new User(
				"test_user",
				"Password1234!",
				"테스트 구매자1",
				"customer1@test.com",
				"010-1234-5678",
				Gender.MALE,
				LocalDate.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				Role.CUSTOMER
		);
	}

	/**
	 * 기본 관리자(Admin) 사용자를 생성합니다.
	 * <p>
	 * 로그인 ID: test_Admin_user<br>
	 * 이메일: admin1@test.com<br>
	 * 역할: ADMIN
	 * </p>
	 *
	 * @return 기본 설정값을 가진 관리자 User 객체
	 */
	public static User defaultAdmin(){
		return new User(
				"test_Admin_user",
				"Password1234!",
				"테스트 관리자1",
				"admin1@test.com",
				"010-1234-5678",
				Gender.MALE,
				LocalDate.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				Role.ADMIN
		);
	}

	/**
	 * 커스텀 로그인 ID를 가진 고객 사용자를 생성합니다.
	 * <p>
	 * 여러 사용자가 필요한 테스트에서 사용합니다.
	 * 이메일은 로그인 ID와 무관하게 customer2@test.com으로 고정됩니다.
	 * </p>
	 *
	 * @param loginId 사용자 로그인 ID
	 * @return 지정된 로그인 ID를 가진 고객 User 객체
	 */
	public static User customer(String loginId){
		return new User(
				loginId,
				"Password1234!",
				"테스트 구매자2",
				"customer2@test.com",
				"010-9999-9999",
				Gender.MALE,
				LocalDate.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				Role.CUSTOMER
		);
	}

	/**
	 * 커스텀 로그인 ID를 가진 관리자 사용자를 생성합니다.
	 * <p>
	 * 여러 관리자가 필요한 테스트에서 사용합니다.
	 * 이메일은 로그인 ID와 무관하게 admin@test.com으로 고정됩니다.
	 * </p>
	 *
	 * @param loginId 관리자 로그인 ID
	 * @return 지정된 로그인 ID를 가진 관리자 User 객체
	 */
	public static User admin(String loginId){
		return new User(
				loginId,
				"Password1234!",
				"테스트 관리자2",
				"admin@test.com",
				"010-8888-8888",
				Gender.MALE,
				LocalDate.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				Role.ADMIN
		);
	}
}
