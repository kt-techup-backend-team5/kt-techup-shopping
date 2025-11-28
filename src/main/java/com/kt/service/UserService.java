package com.kt.service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import com.kt.dto.user.UserResponse;
import com.kt.dto.user.UserUpdatePasswordRequest;
import com.kt.dto.user.UserUpdateRequest;
import com.kt.security.DefaultCurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
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
		// 아이디 중복 체크
		Preconditions.validate(!isDuplicateLoginId(request.loginId()), ErrorCode.ALREADY_EXISTS_USER_ID);
		// 이메일 중복 체크 (나중에 이메일 인증까지 구현)
		Preconditions.validate(!isDuplicateEmail(request.email()), ErrorCode.ALREADY_EXISTS_EMAIL);

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

	public boolean isDuplicateEmail(String email) {
		return userRepository.existsByEmail(email);
	}

	public String findLoginId(String name, String email) {
		var user = userRepository.findByNameAndEmailOrThrow(name, email, ErrorCode.NOT_FOUND_USER);
		return user.getLoginId();
	}

    public void changePassword(Long userId, UserUpdatePasswordRequest request) {

        User user = userRepository.findByIdOrThrow(userId, ErrorCode.NOT_FOUND_USER);

        boolean matchesCurrent = passwordEncoder.matches(request.oldPassword(), user.getPassword());
        Preconditions.validate(matchesCurrent, ErrorCode.DOES_NOT_MATCH_OLD_PASSWORD);

        Preconditions.validate(
                request.newPassword().equals(request.confirmPassword()),
                ErrorCode.CAN_NOT_ALLOWED_SAME_PASSWORD
        );

        Preconditions.validate(
                !passwordEncoder.matches(request.newPassword(), user.getPassword()),
                ErrorCode.CAN_NOT_ALLOWED_SAME_PASSWORD
        );
        String encoded = passwordEncoder.encode(request.newPassword());
        user.changePassword(encoded);;
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

	public void withdrawal(Long id) {
        // 회원 조회
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(()->new CustomException(ErrorCode.NOT_FOUND_USER));
        user.markAsDeleted();
//        userRepository.deleteById(id);
		// 삭제에는 두가지 개념 - softdelete, harddelete
		//var user = userRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
		// userRepository.delete(user);
	}

    public UserResponse.Detail getCurrentUserInfo() {
        DefaultCurrentUser currentUser =
                (DefaultCurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        return UserResponse.Detail.of(user);
    }

    @Transactional
    public UserResponse.Detail updateCurrentUser(UserUpdateRequest request) {
        DefaultCurrentUser currentUser =
                (DefaultCurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.update(request.name(), request.email(), request.mobile());

        return UserResponse.Detail.of(user);
    }

	public void getOrders(Long id) {
		var user = userRepository.findByIdOrThrow(id, ErrorCode.NOT_FOUND_USER);
		var page = orderRepository.findAllByUserId(user.getId(), Pageable.unpaged());
		var orders = page.getContent();

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
