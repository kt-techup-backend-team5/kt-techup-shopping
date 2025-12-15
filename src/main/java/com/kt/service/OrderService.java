package com.kt.service;

import com.kt.dto.order.OrderCancelDecisionRequest;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Lock;
import com.kt.common.support.Message;
import com.kt.common.support.Preconditions;
import com.kt.domain.order.Order;
import com.kt.domain.order.Receiver;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.dto.order.OrderResponse;
import com.kt.dto.order.OrderSearchCondition;
import com.kt.dto.order.OrderStatusUpdateRequest;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;
import com.kt.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import com.kt.domain.refund.Refund;
import com.kt.domain.refund.RefundStatus;
import com.kt.domain.refund.RefundType;
import com.kt.dto.refund.RefundRejectRequest;
import com.kt.dto.refund.RefundRequest;
import com.kt.dto.refund.RefundResponse;
import com.kt.repository.refund.RefundRepository;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;
	private final OrderProductRepository orderProductRepository;
	private final RefundRepository refundRepository;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final StockService stockService;

	// reference , primitive
	// 선택하는 기준 1번째 : null 가능?
	// Long -> null, long -> 0
	// Generic이냐 아니냐 -> Generic은 무조건 참조형
	//주문생성
	@Lock(key = Lock.Key.STOCK, index = 1)
	public void create(
			Long userId,
			Long productId,
			String receiverName,
			String receiverAddress,
			String receiverMobile,
			Long quantity
	) {
		var product = productRepository.findByIdOrThrow(productId);

		// 2. 여기서 획득
		System.out.println(product.getStock());
		Preconditions.validate(product.canProvide(quantity), ErrorCode.NOT_ENOUGH_STOCK);

		var user = userRepository.findByIdOrThrow(userId);

		var receiver = new Receiver(
				receiverName,
				receiverAddress,
				receiverMobile
		);

		var order = orderRepository.save(Order.create(receiver, user));
		var orderProduct = orderProductRepository.save(new OrderProduct(order, product, quantity));

		// 주문생성완료
		product.decreaseStock(quantity);

		product.mapToOrderProduct(orderProduct);
		order.mapToOrderProduct(orderProduct);
		applicationEventPublisher.publishEvent(
				new Message("User: " + user.getName() + " ordered :" + quantity * product.getPrice())
		);
	}

	public void requestCancelByUser(Long orderId, CurrentUser currentUser, String reason) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);
		// '주문'에 기록된 사용자 ID와 '현재 요청한' 사용자 ID를 바로 비교
		Preconditions.validate(
				order.getUser()
						.getId()
						.equals(currentUser.getId()), ErrorCode.NO_AUTHORITY_TO_CANCEL_ORDER);
		order.requestCancel(reason);
	}

	public void requestRefundByUser(Long orderId, CurrentUser currentUser, RefundRequest request) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);
		Preconditions.validate(
				order.getUser().getId().equals(currentUser.getId()),
				ErrorCode.NO_AUTHORITY_TO_REFUND
		);
		Preconditions.validate(order.isRefundable(), ErrorCode.INVALID_ORDER_STATUS);

		// 중복 환불 방지: 이미 완료된 환불이 있는지 확인
		Preconditions.validate(!refundRepository.hasCompletedRefund(order), ErrorCode.ALREADY_REFUNDED);

		Refund refund = new Refund(order, request.getRefundType(), request.getReason());
		refundRepository.save(refund);

		// 환불/반품 요청 시 주문 상태는 변경하지 않음 (Refund 도메인에서 독립적으로 관리)
		// 향후 이벤트 기반 아키텍처로 전환 시 RefundRequestedEvent 발행 가능
	}

	// TODO(seulgi): 취소 요청 기능은 Refund 도메인으로 이동 예정
	// 현재는 즉시 취소 처리로 변경되어 이 메서드는 사용되지 않음
	@Deprecated
	public void decideCancel(Long orderId, OrderCancelDecisionRequest request) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);
		// 즉시 취소로 변경되어 승인 프로세스 제거됨
		throw new UnsupportedOperationException("취소는 즉시 처리됩니다. requestCancelByUser를 사용하세요.");
	}

	@Transactional(readOnly = true)
	@Deprecated
	public Page<OrderResponse.AdminSummary> getOrdersWithCancelRequested(Pageable pageable) {
		// CANCEL_REQUESTED 상태 제거로 인해 사용 불가
		throw new UnsupportedOperationException("취소 요청 조회 기능은 Refund 도메인으로 이동 예정");
	}

	@Transactional(readOnly = true)
	public Page<RefundResponse> getRefunds(Pageable pageable) {
		return refundRepository.findAll(pageable).map(RefundResponse::of);
	}


	public void approveRefund(Long orderId) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);
		Refund refund = refundRepository.findRefundRequestByOrderOrThrow(order);

		// Refund 도메인에서 독립적으로 상태 관리
		refund.approve();

		// 재고 복원 (환불/반품 승인 시)
		if (refund.getType() == RefundType.REFUND) {
			// 배송 전 환불이므로 재고 복원
			order.getOrderProducts().forEach(op -> stockService.increaseStockWithLock(op.getProduct().getId(), op.getQuantity()));
		} else { // RETURN
			// TODO: 반품된 상품의 상태 확인 후 재고 복원 여부 결정 필요 (일단 복원)
			order.getOrderProducts().forEach(op -> stockService.increaseStockWithLock(op.getProduct().getId(), op.getQuantity()));
		}

		// 환불/반품 처리 완료
		refund.complete();

		// TODO: 실제 결제 취소/환불 API 호출
		// 향후 이벤트 기반 아키텍처로 전환 시 RefundCompletedEvent 발행 가능
	}

	public void rejectRefund(Long refundId, RefundRejectRequest request) {
		Refund refund = refundRepository.findByIdOrThrow(refundId);

		// Refund 도메인에서 독립적으로 상태 관리 (reject 메서드 내부에서 검증)
		refund.reject(request.getReason());

		// 환불/반품 거절 시 주문 상태는 변경하지 않음 (Refund 도메인에서 독립적으로 관리)
		// 향후 이벤트 기반 아키텍처로 전환 시 RefundRejectedEvent 발행 가능
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse.AdminSummary> getAdminOrders(OrderSearchCondition condition, Pageable pageable) {
		Page<Order> orders = orderRepository.findByConditions(condition, pageable);

		return orders.map(order -> {
			String firstProductName = order.getOrderProducts().stream()
					.findFirst()
					.map(orderProduct -> orderProduct.getProduct().getName())
					.orElse(null);
			int productCount = order.getOrderProducts().size();

			return new OrderResponse.AdminSummary(
					order.getId(),
					order.getTotalPrice(),
					order.getCreatedAt(),
					order.getStatus(),
					firstProductName,
					productCount,
					order.getUser().getId(),
					order.getUser().getName()
			);
		});
	}

	@Transactional(readOnly = true)
	public OrderResponse.AdminDetail getAdminOrderDetail(Long orderId) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);

		List<OrderResponse.Item> items = order.getOrderProducts().stream()
				.map(op -> new OrderResponse.Item(
						op.getProduct().getId(),
						op.getProduct().getName(),
						op.getProduct().getPrice(),
						op.getQuantity(),
						op.getProduct().getPrice() * op.getQuantity()
				))
				.toList();

		return new OrderResponse.AdminDetail(
				order.getId(),
				order.getReceiver().getName(),
				order.getReceiver().getAddress(),
				order.getReceiver().getMobile(),
				items,
				order.getTotalPrice(),
				order.getStatus(),
				order.getCreatedAt(),
				order.getUser().getId(),
				order.getUser().getName()
		);
	}

	public void changeOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);
		order.changeStatus(request.status());
	}
}

