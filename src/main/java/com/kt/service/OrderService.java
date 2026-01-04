package com.kt.service;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Message;
import com.kt.common.support.Preconditions;

import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.order.Receiver;
import com.kt.domain.order.event.OrderEvent;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.payment.Payment;
import com.kt.domain.product.ProductStatus;
import com.kt.domain.refund.Refund;
import com.kt.domain.refund.RefundType;
import com.kt.domain.refund.event.RefundEvent;

import com.kt.dto.order.*;
import com.kt.dto.refund.RefundRejectRequest;
import com.kt.dto.refund.RefundRequest;
import com.kt.dto.refund.RefundResponse;

import com.kt.repository.address.AddressRepository;
import com.kt.repository.cart.CartItemRepository;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.payment.PaymentRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.refund.RefundRepository;
import com.kt.repository.user.UserRepository;
import com.kt.security.CurrentUser;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;
	private final OrderProductRepository orderProductRepository;
	private final AddressRepository addressRepository;
	private final CartItemRepository cartItemRepository;
	private final RefundRepository refundRepository;
	private final PaymentRepository paymentRepository;

	private final StockService stockService;
	private final PointService pointService;
	private final ApplicationEventPublisher applicationEventPublisher;

	public void create(Long userId, OrderRequest.Create request) {
		var user = userRepository.findByIdOrThrow(userId);
		var address = addressRepository.findByIdAndUserIdOrThrow(request.addressId(), userId);

		var receiver = new Receiver(
				address.getName(),
				address.getMobile(),
				address.getZipcode(),
				address.getAddress(),
				address.getDetailAddress()
		);

        var deliveryRequest = (request.deliveryRequest() != null) ? request.deliveryRequest() : "";
        var order = orderRepository.save(Order.create(receiver, user, deliveryRequest));

		for (var item : request.items()) {
            var productId = item.productId();
            var quantity = item.quantity();
            var product = productRepository.findByIdOrThrow(productId);

            Preconditions.validate(product.getStatus() == ProductStatus.ACTIVATED, ErrorCode.NOT_ON_SALE_PRODUCT);

            stockService.decreaseStockWithLock(productId, quantity);

            var orderProduct = orderProductRepository.save(
                    new OrderProduct(order, product, quantity)
            );

            product.mapToOrderProduct(orderProduct);
            order.mapToOrderProduct(orderProduct);
        }

		// 포인트 사용 처리
        Long usePoints = request.usePoints();
		if (usePoints != null && usePoints > 0) {
            Preconditions.validate(usePoints <= order.getTotalPrice(), ErrorCode.INVALID_POINT_AMOUNT);

			order.setUsedPoints(usePoints);  // Order에 사용 포인트 저장
			pointService.usePoints(userId, order.getId(), usePoints);
		}

        // CART 주문이면 장바구니 정리
        if (request.orderType() == OrderRequest.OrderType.CART) {
            var productIds = request.items().stream()
                    .map(OrderRequest.OrderItem::productId)
                    .distinct()
                    .toList();

            cartItemRepository.deleteByUserIdAndProductIdIn(userId, productIds);
        }

		applicationEventPublisher.publishEvent(
                new Message(String.format(
                        "[주문 생성] orderId=%d / userId=%d / 총액=%d원 / 상품수=%d",
                        order.getId(), userId, order.getTotalPrice(), request.items().size()
                ))
        );
	}

	public void requestCancelByUser(Long orderId, CurrentUser currentUser, String reason) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId);
		// '주문'에 기록된 사용자 ID와 '현재 요청한' 사용자 ID를 바로 비교
		Preconditions.validate(
				order.getUser()
						.getId()
						.equals(currentUser.getId()), ErrorCode.NO_AUTHORITY_TO_CANCEL_ORDER);
		order.requestCancel(reason);
	}

	public void requestRefundByUser(Long orderId, CurrentUser currentUser, RefundRequest request) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId);
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
		Order order = orderRepository.findByOrderIdOrThrow(orderId);
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
		Order order = orderRepository.findByOrderIdOrThrow(orderId);
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

		// 환불 승인 이벤트 발행 (포인트 회수 트리거)
		applicationEventPublisher.publishEvent(
			new RefundEvent.Approved(
				refund.getId(),
				orderId,
				order.getUser().getId()
			)
		);

		// TODO: 실제 결제 취소/환불 API 호출
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
		Order order = orderRepository.findByOrderIdOrThrow(orderId);
		Receiver receiver = order.getReceiver();

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
				receiver.getName(),
				receiver.getMobile(),
				receiver.getZipcode(),
				receiver.getAddress(),
				receiver.getDetailAddress(),
				order.getDeliveryRequest(),
				items,
				order.getTotalPrice(),
				order.getUsedPoints(),
				order.getStatus(),
				order.getCreatedAt(),
				order.getUser().getId(),
				order.getUser().getName()
		);
	}

	public void changeOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId);
		order.changeStatus(request.status());
	}

	/**
	 * 구매 확정
	 * 사용자가 상품을 받고 구매 확정하면 포인트 적립
	 */
	public void confirmOrder(Long orderId, CurrentUser currentUser) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId);

		// 권한 확인
		Preconditions.validate(
			order.getUser().getId().equals(currentUser.getId()),
			ErrorCode.NO_AUTHORITY_TO_CANCEL_ORDER
		);

		// 배송 완료 상태에서만 구매 확정 가능
		Preconditions.validate(
			order.getStatus() == OrderStatus.ORDER_DELIVERED,
			ErrorCode.INVALID_ORDER_STATUS
		);

		// 주문 상태 변경
		order.changeStatus(OrderStatus.ORDER_CONFIRMED);

		// 실결제 금액 조회
		Payment payment = paymentRepository.findByOrderOrThrow(order);
		Long actualPaymentAmount = payment.getFinalPrice();

		// 구매 확정 이벤트 발행 (포인트 적립 트리거)
		applicationEventPublisher.publishEvent(
			new OrderEvent.Confirmed(orderId, currentUser.getId(), actualPaymentAmount)
		);
	}
}

