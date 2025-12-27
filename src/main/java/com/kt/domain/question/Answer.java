package com.kt.domain.question;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.BaseEntity;
import com.kt.common.support.Preconditions;
import com.kt.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Answer extends BaseEntity {

	@Lob
	@Column(nullable = false)
	private String content;

	@ManyToOne
	@JoinColumn(name = "admin_id", nullable = false)
	private User admin;

	@OneToOne
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	public Answer(String content, User admin, Question question) {
		Preconditions.validate(content != null && !content.isBlank(), ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(admin != null, ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(question != null, ErrorCode.INVALID_PARAMETER);

		this.content = content;
		this.admin = admin;
		this.question = question;

		// 양방향 관계 설정: Question의 상태를 답변 완료로 변경
		question.markAsAnswered();
	}

	// 답변 내용 수정
	public void updateContent(String content) {
		Preconditions.validate(content != null && !content.isBlank(), ErrorCode.INVALID_PARAMETER);
		this.content = content;
	}

	// 작성자 확인 (관리자)
	public boolean isWrittenBy(Long adminId) {
		return this.admin.getId().equals(adminId);
	}
}
