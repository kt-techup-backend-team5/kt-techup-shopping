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

	private Long paymentId;

	private Long usedPoints = 0L;  // 주문 시 사용한 포인트

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
		this.status = OrderStatus.ORDER_CREATED;
	}

	public static Order create(Receiver receiver, User user) {
		return new Order(
			receiver,
			user
		);
	}

	public void setUsedPoints(Long usedPoints) {
		this.usedPoints = usedPoints != null ? usedPoints : 0L;
	}

	public void mapToOrderProduct(OrderProduct orderProduct) {
		this.orderProducts.add(orderProduct);
	}

    public void changeReceiver(String name, String address, String mobile) {
        this.receiver = new Receiver(name, address, mobile);
    }

    public boolean canUpdate() {
        return this.status == OrderStatus.ORDER_CREATED || this.status == OrderStatus.ORDER_ACCEPTED;
    }

	// TODO(seulgi): 여기 주문 취소부분은 다른 서비스로 들어가야 될것같음. refund로. 일단은 그냥 둠
	public void requestCancel(String reason) {
		var cancellableStates = List.of(OrderStatus.ORDER_CREATED, OrderStatus.ORDER_ACCEPTED, OrderStatus.ORDER_PREPARING);
		Preconditions.validate(cancellableStates.contains(this.status), ErrorCode.CANNOT_CANCEL_ORDER);
		this.previousStatus = this.status;
		this.userCancelReason = reason;
		// 즉시 취소 처리 (관리자 승인 불필요)
		this.status = OrderStatus.ORDER_CANCELLED;
	}

	public long getTotalPrice() {
		return orderProducts.stream()
			.mapToLong(op -> op.getProduct().getPrice() * op.getQuantity())
			.sum();
	}

	/**
	 * 결제 성공 이벤트 수신 시 호출
	 */
	public void acceptPayment(Long paymentId) {
		Preconditions.validate(this.status == OrderStatus.ORDER_CREATED, ErrorCode.INVALID_ORDER_STATUS);
		this.paymentId = paymentId;
		this.status = OrderStatus.ORDER_ACCEPTED;
	}

	/**
	 * 결제 실패 이벤트 수신 시 호출
	 */
	public void cancelByPaymentFailure() {
		Preconditions.validate(this.status == OrderStatus.ORDER_CREATED, ErrorCode.INVALID_ORDER_STATUS);
		this.status = OrderStatus.ORDER_CANCELLED;
	}

	/**
	 * @deprecated 이벤트 기반 결제로 전환. acceptPayment() 사용 권장
	 */
	@Deprecated
	public void setPaid() {
		this.status = OrderStatus.ORDER_ACCEPTED;
	}

	public void changeStatus(OrderStatus orderStatus){
		this.status = orderStatus;
	}

	public boolean isRefundable() {
		return List.of(OrderStatus.ORDER_SHIPPING, OrderStatus.ORDER_DELIVERED).contains(this.status);
	}

}
