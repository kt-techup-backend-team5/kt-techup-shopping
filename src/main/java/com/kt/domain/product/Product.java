package com.kt.domain.product;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Strings;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.BaseEntity;
import com.kt.common.support.Preconditions;
import com.kt.domain.orderproduct.OrderProduct;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {
	private String name;
	private Long price;
	private Long stock;
	private Long viewCount;
	@Lob
	private String description;
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private ProductStatus status = ProductStatus.ACTIVATED;
	@OneToMany(mappedBy = "product")
	@Builder.Default
	private List<OrderProduct> orderProducts = new ArrayList<>();

	public Product(String name, Long price, Long stock, String description) {
		Preconditions.validate(Strings.isNotBlank(name), ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(price != null, ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(price >= 0, ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(stock != null, ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(stock >= 0, ErrorCode.INVALID_PARAMETER);

		this.name = name;
		this.price = price;
		this.stock = stock;
		this.viewCount = 0L;
		this.description = description;
		this.status = ProductStatus.ACTIVATED;
	}

	public void update(String name, Long price, Long stock, String description) {
		this.name = name;
		this.price = price;
		this.stock = stock;
		this.description = description;
		this.updatedAt = LocalDateTime.now();
	}

	public void soldOut() {
		this.status = ProductStatus.SOLD_OUT;
		this.updatedAt = LocalDateTime.now();
	}

	public void inActivate() {
		this.status = ProductStatus.IN_ACTIVATED;
		this.updatedAt = LocalDateTime.now();
	}

	public void activate() {
		this.status = ProductStatus.ACTIVATED;
		this.updatedAt = LocalDateTime.now();
	}

	public void delete() {
		// 논리삭제
		this.status = ProductStatus.DELETED;
		this.updatedAt = LocalDateTime.now();
	}

	public void decreaseStock(Long quantity) {
		this.stock -= quantity;
	}

	public void increaseStock(Long quantity) {
		this.stock += quantity;
	}

	public boolean canProvide(Long quantity) {
		return this.stock >= quantity;
	}

	public void mapToOrderProduct(OrderProduct orderProduct) {
		this.orderProducts.add(orderProduct);
	}

	public void addViewCountIncrement(Long viewCountIncrement) {
		this.viewCount += viewCountIncrement;
	}
}
