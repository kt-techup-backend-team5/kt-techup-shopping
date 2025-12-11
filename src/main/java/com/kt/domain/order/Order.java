package com.kt.domain.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kt.common.support.BaseEntity;
import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.user.User;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor
public class Order extends BaseEntity {
	@Embedded
	private Receiver receiver;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private OrderStatus previousStatus;

	private String cancelDecisionReason;
	private String userCancelReason;

	private LocalDateTime deliveredAt;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@OneToMany(mappedBy = "order")
	private List<OrderProduct> orderProducts = new ArrayList<>();

	public Order(Receiver receiver, User user) {
		this.receiver = receiver;
		this.user = user;
		this.deliveredAt = LocalDateTime.now().plusDays(3);
		this.status = OrderStatus.PENDING;
	}

	public static Order create(Receiver receiver, User user) {
		return new Order(
			receiver,
			user
		);
	}

	public void mapToOrderProduct(OrderProduct orderProduct) {
		this.orderProducts.add(orderProduct);
	}

    public void changeReceiver(String name, String address, String mobile) {
        this.receiver = new Receiver(name, address, mobile);
    }

    public boolean canUpdate() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.COMPLETED;
    }

	public void requestCancel(String reason) {

		var cancellableStates = List.of(OrderStatus.PENDING, OrderStatus.COMPLETED, OrderStatus.PREPARING);

		Preconditions.validate(cancellableStates.contains(this.status), ErrorCode.CANNOT_CANCEL_ORDER);

		this.previousStatus = this.status;
		this.status = OrderStatus.CANCEL_REQUESTED;
		this.userCancelReason = reason;
	}

    public void approveCancel(String reason) {
        Preconditions.validate(isCancelRequestableByAdmin(), ErrorCode.INVALID_ORDER_STATUS);
        this.status = OrderStatus.CANCELLED;
        this.cancelDecisionReason = reason;
    }

    public void rejectCancel(String reason) {
        Preconditions.validate(isCancelRequestableByAdmin(), ErrorCode.INVALID_ORDER_STATUS);
        this.status = this.previousStatus;
        this.cancelDecisionReason = reason;
    }

    public boolean isCancelRequestableByAdmin() {
        return this.status == OrderStatus.CANCEL_REQUESTED;
    }

	public long getTotalPrice() {
		return orderProducts.stream()
			.mapToLong(op -> op.getProduct().getPrice() * op.getQuantity())
			.sum();
	}

	public void setPaid() {
		this.status = OrderStatus.COMPLETED;
	}

	public void changeStatus(OrderStatus orderStatus){
		this.status = orderStatus;
	}

	public boolean isRefundable() {
		return List.of(OrderStatus.COMPLETED, OrderStatus.SHIPPING, OrderStatus.DELIVERED).contains(this.status);
	}

}
