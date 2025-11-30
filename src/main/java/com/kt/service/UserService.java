package com.kt.service;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.user.User;
import com.kt.dto.user.UserCreateRequest;
import com.kt.dto.user.UserResponse;
import com.kt.dto.user.UserUpdatePasswordRequest;
import com.kt.dto.user.UserUpdateRequest;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.user.UserRepository;
import com.kt.security.DefaultCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final OrderRepository orderRepository;

	public void create(UserCreateRequest request) {
		Preconditions.validate(!isDuplicateLoginId(request.loginId()), ErrorCode.ALREADY_EXISTS_USER_ID);
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

	public void createAdmin(UserCreateRequest request) {
		Preconditions.validate(!userRepository.existsByLoginId(request.loginId()), ErrorCode.ALREADY_EXISTS_USER_ID);

		var newAdmin = User.admin(
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
		userRepository.save(newAdmin);
	}

	public boolean isDuplicateLoginId(String loginId) {
		return userRepository.existsByLoginId(loginId);
	}

	public boolean isDuplicateEmail(String email) {
		return userRepository.existsByEmail(email);
	}

	public String findLoginId(String name, String email) {
		var user = userRepository.findByNameAndEmailOrThrow(name, email);
		return user.getLoginId();
	}

	public void changePassword(Long userId, UserUpdatePasswordRequest request) {
		User user = userRepository.findByIdOrThrow(userId);

		boolean matchesCurrent = passwordEncoder.matches(request.oldPassword(), user.getPassword());
		Preconditions.validate(matchesCurrent, ErrorCode.DOES_NOT_MATCH_OLD_PASSWORD);

		Preconditions.validate(
				request.newPassword().equals(request.confirmPassword()),
				ErrorCode.NOT_MATCHED_CHECK_PASSWORD
		);

		Preconditions.validate(
				!passwordEncoder.matches(request.newPassword(), user.getPassword()),
				ErrorCode.CAN_NOT_ALLOWED_SAME_PASSWORD
		);
		String encoded = passwordEncoder.encode(request.newPassword());
		user.changePassword(encoded);
	}

	public Page<User> search(Pageable pageable, String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return userRepository.findAll(pageable);
		}
		return userRepository.findAllByNameContaining(keyword, pageable);
	}

	public User detail(Long id) {
		return userRepository.findByIdOrThrow(id);
	}

	@Transactional
	public UserResponse.Detail update(Long id, String name, String email, String mobile) {
		var user = userRepository.findByIdOrThrow(id);
		user.update(name, email, mobile);
		return UserResponse.Detail.of(user);
	}

	public void withdrawal(Long id) {
		User user = userRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
		user.deleted();
	}

	@Transactional
	public void deactivateUser(Long id) {
		User user = userRepository.findByIdOrThrow(id);
		user.deleted();
	}

	@Transactional
	public void activateUser(Long id) {
		User user = userRepository.findByIdIncludeDeletedOrThrow(id);
		user.activate();
	}

	public UserResponse.Detail getCurrentUserInfo() {
		DefaultCurrentUser currentUser =
				(DefaultCurrentUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Long userId = currentUser.getId();

		User user = userRepository.findByIdOrThrow(currentUser.getId());

		return UserResponse.Detail.of(user);
	}

	@Transactional
	public UserResponse.Detail updateCurrentUser(UserUpdateRequest request) {
		DefaultCurrentUser currentUser =
				(DefaultCurrentUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		User user = userRepository.findByIdOrThrow(currentUser.getId());

		user.update(request.name(), request.email(), request.mobile());

		return UserResponse.Detail.of(user);
	}

	public void getOrders(Long id) {
		var user = userRepository.findByIdOrThrow(id);
		var page = orderRepository.findAllByUserId(user.getId(), Pageable.unpaged());
		var orders = page.getContent();

		var products = orders.stream()
				.flatMap(order -> order.getOrderProducts().stream()
						.map(orderProduct -> orderProduct.getProduct().getName())).toList();
	}

	public void grantAdminRole(Long id) {
		var user = userRepository.findByIdOrThrow(id);
		user.grantAdminRole();
	}

	public void revokeAdminRole(Long id) {
		var user = userRepository.findByIdOrThrow(id);
		user.revokeAdminRole();
	}
}
