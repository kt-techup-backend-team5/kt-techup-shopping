package com.kt.service;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.ErrorCode;
import com.kt.common.Lock;
import com.kt.common.Preconditions;
import com.kt.domain.order.Order;
import com.kt.domain.order.Receiver;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
	private final RedisProperties redisProperties;
	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;
	private final OrderProductRepository orderProductRepository;
	private final RedissonClient redissonClient;

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
		// var product = productRepository.findByIdPessimistic(productId).orElseThrow();
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
	}
}
