package com.kt.service;

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
import com.kt.domain.user.Role;
import com.kt.dto.order.OrderResponse;
import com.kt.dto.order.OrderSearchCondition;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;
import com.kt.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;
	private final OrderProductRepository orderProductRepository;
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

		var user = userRepository.findByIdOrThrow(userId, ErrorCode.NOT_FOUND_USER);

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

	public void cancelOrder(Long orderId, CurrentUser currentUser) {
		Order order = orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);

		var requestingUser = userRepository.findByIdOrThrow(currentUser.getId(), ErrorCode.NOT_FOUND_USER);

		Preconditions.validate(
				requestingUser.getRole() == Role.ADMIN || order.getUser().getId().equals(currentUser.getId()),
				ErrorCode.NO_AUTHORITY_TO_CANCEL_ORDER);

		order.cancel();

		for (OrderProduct orderProduct : order.getOrderProducts()) {
			stockService.increaseStockWithLock(orderProduct.getProduct().getId(), orderProduct.getQuantity());
		}
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse.AdminSummary> getAdminOrders(OrderSearchCondition condition, Pageable pageable) {
		Page<Order> orders = orderRepository.findByConditions(condition, pageable);

		return orders.map(order -> {
			String firstProductName = null;
			int productCount = 0;
			if (!order.getOrderProducts().isEmpty()) {
				firstProductName = order.getOrderProducts().get(0).getProduct().getName();
				productCount = order.getOrderProducts().size();
			}

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
}
