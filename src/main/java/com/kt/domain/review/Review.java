package com.kt.domain.review;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;

import com.kt.common.support.BaseEntity;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;
import com.kt.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor
@SQLDelete(sql = "UPDATE reviews SET deleted = true, deleted_at = NOW() WHERE id = ?")
public class Review extends BaseEntity {
	private String content;
	private int rating;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_product_id")
	private OrderProduct orderProduct;

	@Column(nullable = false)
	private boolean deleted = false;
	private LocalDateTime deletedAt;

	public Review(String content, int rating, User user, Product product, OrderProduct orderProduct) {
		this.content = content;
		this.rating = rating;
		this.user = user;
		this.product = product;
		this.orderProduct = orderProduct;
	}

	public void update(String content, int rating) {
		this.content = content;
		this.rating = rating;
		this.updatedAt = LocalDateTime.now();
	}
}