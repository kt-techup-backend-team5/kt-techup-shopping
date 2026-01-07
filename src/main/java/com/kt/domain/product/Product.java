package com.kt.domain.product;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Strings;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.BaseEntity;
import com.kt.common.support.Preconditions;
import com.kt.domain.orderproduct.OrderProduct;

import jakarta.persistence.Embedded;
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
	private String thumbnailImgUrl;
	private String detailImgUrl;
	@Embedded
	private ProductAnalysis productAnalysis;

	public Product(String name, Long price, Long stock, String description, String thumbnailImgUrl,
			String detailImgUrl, ProductAnalysis productAnalysis) {
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
		this.thumbnailImgUrl = thumbnailImgUrl;
		this.detailImgUrl = detailImgUrl;
		this.productAnalysis = productAnalysis;
	}

	public void update(String name, Long price, Long stock, String description, String thumbnailImgUrl,
			String detailImgUrl) {
		this.name = name;
		this.price = price;
		this.stock = stock;
		this.description = description;
		this.thumbnailImgUrl = thumbnailImgUrl;
		this.detailImgUrl = detailImgUrl;
	}

	public void soldOut() {
		this.status = ProductStatus.SOLD_OUT;
	}

	public void inActivate() {
		this.status = ProductStatus.IN_ACTIVATED;
	}

	public void activate() {
		this.status = ProductStatus.ACTIVATED;
	}

	public void delete() {
		// 논리삭제
		this.status = ProductStatus.DELETED;
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
