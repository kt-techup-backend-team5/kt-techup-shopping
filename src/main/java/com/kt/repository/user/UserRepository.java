package com.kt.repository.user;

import java.util.Optional;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.user.Role;
import com.kt.domain.user.User;

import jakarta.validation.constraints.NotNull;

public interface UserRepository extends JpaRepository<User, Long> {

    interface AuthInfo {
        Role getRole();

        String getLoginId();
    }

	Boolean existsByEmail(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

	Optional<User> findByLoginId(String loginId);

	Optional<User> findByNameAndEmail(String name, String email);

	default User findByNameAndEmailOrThrow(String name, String email) {
		return findByNameAndEmail(name, email)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
	}

	@Query("""
			SELECT exists (SELECT u FROM User u WHERE u.loginId = ?1)
		""")
	Boolean existsByLoginId(String loginId);

    Page<User> findAllByRole(Role role, Pageable pageable);

	Page<User> findByRoleIn(Collection<Role> roles, Pageable pageable);

	Page<User> findByNameContaining(String name, Pageable pageable);

	Page<User> findByRoleInAndNameContaining(Collection<Role> roles, String name, Pageable pageable);

	@Query(value = """
			SELECT DISTINCT u FROM User u
			LEFT JOIN FETCH u.orders o
			WHERE u.id = :id
		""")
	@NotNull
	Optional<User> findById(@NotNull Long id);

	default User findByIdOrThrow(Long id) {
		return findById(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
	}

	@Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
	Optional<User> findByIdIncludeDeleted(@Param("id") Long id);

    @Query("SELECT u.role AS role, u.loginId AS loginId FROM User u WHERE u.id = :id")
    Optional<AuthInfo> findAuthInfoById(@Param("id") Long id);

	default User findByIdIncludeDeletedOrThrow(Long id) {
		return findByIdIncludeDeleted(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
	}
}
