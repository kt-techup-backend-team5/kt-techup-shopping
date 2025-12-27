package com.kt.domain.question;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.BaseEntity;
import com.kt.common.support.Preconditions;
import com.kt.domain.product.Product;
import com.kt.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Question extends BaseEntity {

	@Lob
	@Column(nullable = false)
	private String content;

	@Column(nullable = false)
	private boolean isPublic;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QuestionStatus status;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@OneToOne(mappedBy = "question")
	private Answer answer;

	public Question(String content, boolean isPublic, User user, Product product) {
		Preconditions.validate(content != null && !content.isBlank(), ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(user != null, ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(product != null, ErrorCode.INVALID_PARAMETER);

		this.content = content;
		this.isPublic = isPublic;
		this.user = user;
		this.product = product;
		this.status = QuestionStatus.PENDING;
	}

	// 문의 내용 수정, 답변이 달린 문의는 수정 불가
	public void updateContent(String content) {
		Preconditions.validate(this.status == QuestionStatus.PENDING, ErrorCode.CANNOT_UPDATE_ANSWERED_QUESTION);
		Preconditions.validate(content != null && !content.isBlank(), ErrorCode.INVALID_PARAMETER);
		this.content = content;
	}

	// 공개 여부 수정
	public void updateIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	// 답변 완료 처리
	public void markAsAnswered() {
		Preconditions.validate(this.status == QuestionStatus.PENDING, ErrorCode.ALREADY_ANSWERED);
		this.status = QuestionStatus.ANSWERED;
	}

	// 답변 대기로 변경 (답변 삭제 시)
	public void markAsPending() {
		Preconditions.validate(this.status == QuestionStatus.ANSWERED, ErrorCode.INVALID_QUESTION_STATUS);
		this.status = QuestionStatus.PENDING;
	}

	// 작성자 확인
	public boolean isOwnedBy(Long userId) {
		return this.user.getId().equals(userId);
	}

	// 삭제 가능 여부 확인 (답변 없을 때만 삭제 가능)
	public boolean canDelete() {
		return this.status == QuestionStatus.PENDING;
	}
}
