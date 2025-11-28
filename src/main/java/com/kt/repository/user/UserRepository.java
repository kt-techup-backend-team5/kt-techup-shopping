package com.kt.repository.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.user.User;

import jakarta.validation.constraints.NotNull;

// <T, ID>
// T: Entity 클래스 => User
// ID: Entity 클래스의 PK 타입 => Long
public interface UserRepository extends JpaRepository<User, Long> {
	//여기에 쿼리를 작성해줘야
	// JPA에서는 쿼리를 작성하는 3가지 방법이 존재
	// 1. 네이티브 쿼리 작성 (3)
	// 2. JPQL 작성 -> 네이티브 쿼리랑 같은데 Entity 기반 (2) - 너무길어진 메소드이름을 그냥 쿼리작성해서 숨김
	// 3. querymethod 작성 -> 메서드 이름을 쿼리 처럼 작성 (1) - 길어지면 상당히 이상해보임
	// 찾는다 : findByXX , 존재하냐? existsByXX, 삭제 : deleteByXX

	Boolean existsByLoginId(String loginId);

	Boolean existsByEmail(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

	Optional<User> findByLoginId(String loginId);

	Optional<User> findByNameAndEmail(String name, String email);

	default User findByNameAndEmailOrThrow(String name, String email, ErrorCode errorCode) {
		return findByNameAndEmail(name, email)
				.orElseThrow(() -> new CustomException(errorCode));
	}

	@Query("""
			SELECT exists (SELECT u FROM User u WHERE u.loginId = ?1)
		""")
	Boolean existsByLoginIdJPQL(String loginId);

	Page<User> findAllByNameContaining(String name, Pageable pageable);

	@Query(value = """
			SELECT DISTINCT u FROM User u
			LEFT JOIN FETCH u.orders o
			WHERE u.id = :id
		""")
	@NotNull
	Optional<User> findById(@NotNull Long id);

	// @EntityGraph(attributePaths = "orders")
	// @NotNull Optional<User> findById(@NotNull Long id);

	default User findByIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id).orElseThrow(() -> new CustomException(errorCode));
	}
}
