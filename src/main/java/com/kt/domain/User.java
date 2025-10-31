package java.com.kt.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
	private Long id;
	private String loginId;
	private String password;
	private String name;
	private String email;
	private String mobile;
	private com.kt.domain.Gender gender;
	private LocalDate birthday;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
