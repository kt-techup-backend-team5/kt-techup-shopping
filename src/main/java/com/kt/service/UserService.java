package com.kt.service;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.user.User;
import com.kt.dto.user.*;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.user.UserRepository;
import com.kt.security.DefaultCurrentUser;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.kt.domain.user.Role;

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

		var newUser = User.customer(
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
		var user = userRepository.findByNameAndEmailOrThrow(name, email);
		return user.getLoginId();
	}

	public void changePassword(Long userId, UserChangePasswordRequest request) {
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

	/**
	 * 임시 비밀번호를 생성하는 메서드입니다.
	 * 최소 8자 이상이며, 영문 소문자, 대문자, 숫자, 특수문자를 각각 1개 이상 포함합니다.
	 * @return 생성된 임시 비밀번호 문자열
	 */
	private String generateRandomPassword() {
		final String lower = "abcdefghijklmnopqrstuvwxyz";
		final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final String digits = "0123456789";
		final String special = "!@#$%^";

		// 모든 문자 세트를 하나로 합칩니다.
		final String allCharacters = lower + upper + digits + special;
		final int passwordLength = 8;
		final SecureRandom random = new SecureRandom();

		List<Character> passwordChars = new ArrayList<>();

		// 1. 각 문자 세트에서 최소 1개의 문자를 보장합니다.
		passwordChars.add(lower.charAt(random.nextInt(lower.length())));
		passwordChars.add(upper.charAt(random.nextInt(upper.length())));
		passwordChars.add(digits.charAt(random.nextInt(digits.length())));
		passwordChars.add(special.charAt(random.nextInt(special.length())));

		// 2. 전체 문자 세트에서 나머지 길이만큼 랜덤하게 문자를 추가합니다.
		for (int i = passwordChars.size(); i < passwordLength; i++) {
			passwordChars.add(allCharacters.charAt(random.nextInt(allCharacters.length())));
		}

		// 3. 생성된 비밀번호의 문자 순서를 무작위로 섞습니다.
		Collections.shuffle(passwordChars, random);

		// 4. 최종 비밀번호를 문자열 형태로 조합합니다.
		StringBuilder password = new StringBuilder(passwordLength);
		for (Character ch : passwordChars) {
			password.append(ch);
		}

		return password.toString();
	}

	@Transactional
	public void changePasswordByAdmin(Long userId, AdminChangePasswordRequest request) {
		User user = userRepository.findByIdOrThrow(userId);
		String encodedPassword = passwordEncoder.encode(request.newPassword());
		user.changePassword(encodedPassword);
	}

	public Page<User> search(Pageable pageable, String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return userRepository.findAll(pageable);
		}
		return userRepository.findAllByNameContaining(keyword, pageable);
	}

	public Page<User> searchAdmins(Pageable pageable) {
		return userRepository.findAllByRole(Role.ADMIN, pageable);
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

		User user = userRepository.findByIdOrThrow(currentUser.getId());

		return UserResponse.Detail.of(user);
	}

    @Transactional
    public UserResponse.Detail changeCurrentUser(UserChangeRequest request) {
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

    @Transactional
    public User getAdminTargetOrThrow(Long id) {
        User user = detail(id);
        Preconditions.validate(
                user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN,
                ErrorCode.USER_NOT_ADMIN
        );
        return user;
    }

    @Transactional
    public void deleteAdmin(Long currentUserId, Long targetUserId) {
        Preconditions.validate(!currentUserId.equals(targetUserId), ErrorCode.CANNOT_DELETE_SELF);
        var user = detail(targetUserId);
        Preconditions.validate(user.getRole() != Role.SUPER_ADMIN, ErrorCode.CANNOT_DELETE_SUPER_ADMIN
        );
        Preconditions.validate(user.getRole() == Role.ADMIN, ErrorCode.USER_NOT_ADMIN);
        deactivateUser(targetUserId);
    }

	public void grantAdminRole(Long id) {
		var user = userRepository.findByIdOrThrow(id);
		user.grantAdminRole();
	}

	public void revokeAdminRole(Long id) {
		var user = userRepository.findByIdOrThrow(id);
		user.revokeAdminRole();
	}

    @Transactional
    public String initAdminPassword(Long targetUserId) {
        var user = detail(targetUserId);
        Preconditions.validate(user.getRole() == Role.ADMIN, ErrorCode.USER_NOT_ADMIN);
        return initPassword(targetUserId);
    }

    @Transactional
	public String initPassword(Long userId) {
		User user = userRepository.findByIdOrThrow(userId);
        String tempPassword = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
		user.changePassword(encodedPassword);
		return tempPassword;
	}
}
