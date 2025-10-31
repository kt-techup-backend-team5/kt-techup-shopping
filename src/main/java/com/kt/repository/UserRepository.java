package java.com.kt.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Repository;

import java.com.kt.domain.User;
import java.com.kt.dto.UserCreateRequest;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepository {
	private final JdbcTemplate jdbcTemplate;

	public void save(User user) {
		var sql = """
			INSERT INTO MEMBER (
			                    id,
			                    loginId, 
			                    password, 
			                    name, 
			                    birthday,
			                    mobile,
			                    email,
			                    gender,
			                    createdAt,
			                    updatedAt
			                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

		jdbcTemplate.update(
			sql,
			user.getId(),
			user.getLoginId(),
			user.getPassword(),
			user.getName(),
			user.getBirthday(),
			user.getMobile(),
			user.getEmail(),
			user.getGender().name(),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}

	public Long selectMaxId() {
		var sql = "SELECT MAX(id) FROM MEMBER";

		var maxId = jdbcTemplate.queryForObject(sql, Long.class);

		return maxId == null ? 0L : maxId;
	}
}
