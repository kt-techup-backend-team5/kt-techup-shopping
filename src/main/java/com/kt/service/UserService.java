package com.kt.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.ErrorCode;
import com.kt.common.Preconditions;
import com.kt.domain.user.User;
import com.kt.dto.user.UserRequest;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

// 구현체가 하나 이상 필요로해야 인터페이스가 의미가있다
// 인터페이스 : 구현체 1:1로 다 나눠야하나
// 관례를 지키려고 추상화를 굳이하는 것을 관습적추상화
// 인터페이스로 굳이 나눴을때 불편한 점

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final OrderRepository orderRepository;

	// 트랜잭션 처리해줘
	// PSA - Portable Service Abstraction
	// 환경설정을 살짝 바꿔서 일관된 서비스를 제공하는 것
	public void create(UserRequest.Create request) {
		var newUser = User.normalUser(
				request.loginId(),
			passwordEncoder.encode(request.password()),
				request.name(),
				request.email(),
				request.mobile(),
				request.gender(),
				request.birthday(),
				LocalDateTime.now(),
				LocalDateTime.now()
			);

			userRepository.save(newUser);
	}

	public boolean isDuplicateLoginId(String loginId) {
		return userRepository.existsByLoginId(loginId);
	}

	public void changePassword(Long id, String oldPassword, String password) {
		var user = userRepository.findByIdOrThrow(id, ErrorCode.NOT_FOUND_USER);

		//검증 작업
		// 긍정적인 상황만 생각하자 -> 패스워드가 이전것과 달라야 => 해피한
		// 패스워드가 같으면 안되는데 => 넌 해피하지 않은 상황
		Preconditions.validate(user.getPassword().equals(oldPassword), ErrorCode.DOES_NOT_MATCH_OLD_PASSWORD);
		Preconditions.validate(!oldPassword.equals(password), ErrorCode.CAN_NOT_ALLOWED_SAME_PASSWORD);

		user.changePassword(password);
	}

	// Pageable 인터페이스
	public Page<User> search(Pageable pageable, String keyword) {
		return userRepository.findAllByNameContaining(keyword, pageable);
	}

	public User detail(Long id) {
		return userRepository.findByIdOrThrow(id, ErrorCode.NOT_FOUND_USER);
	}

	public void update(Long id, String name, String email, String mobile) {
		var user = userRepository.findByIdOrThrow(id, ErrorCode.NOT_FOUND_USER);

		user.update(name, email, mobile);
	}

	public void delete(Long id) {
		userRepository.deleteById(id);
		// 삭제에는 두가지 개념 - softdelete, harddelete
		// var user = userRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
		// userRepository.delete(user);
	}

	public void getOrders(Long id) {
		var user = userRepository.findByIdOrThrow(id, ErrorCode.NOT_FOUND_USER);
		var orders = orderRepository.findAllByUserId(user.getId());

		var products = orders.stream()
			.flatMap(order -> order.getOrderProducts().stream()
				.map(orderProduct -> orderProduct.getProduct().getName())).toList();

		// var statuses = orders.stream()
		// 	.flatMap(order -> order.getOrderProducts().stream()
		// 		.map(orderProduct -> orderProduct.getOrder().getStatus())).toList();

		// N개의 주문이 있는데 N개의 주문엔 상품이 존재하는데 가짓수가 1만개

		// Stream의 연산과정
		// 1. 스트림생성
		// 2. 중간연산 -> 여러번 가능 O
		// 3. 최종연산 -> 여러번 가능 X -> 재사용 불가능

		// List<List<Product>> -> List<Product>

		// N + 1 문제를 해결하는 방법
		// 1. fetch join 사용 -> JPQL전용 -> 딱 1번 사용 2번사용하면 에러남
		// 2. @EntityGraph 사용 -> JPA표준기능 -> 여러번 사용가능
		// 3. batch fetch size 옵션 사용 -> 전역설정 -> paging동작원리와 같아서 성능이슈가 있을 수 있음
		// 4. @BatchSize 어노테이션 사용 -> 특정 엔티티에만 적용 가능
		// 5. native query 사용해서 해결

		// Collection, stream, foreach

		// 연관관계를 아예 끊는다 -> 엔티티자체를 느슨하게 결합해둔다.
		// JPA를 안쓴다.
	}
}
