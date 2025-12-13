package com.kt.service;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.order.Receiver;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;
import com.kt.domain.user.Gender;
import com.kt.domain.user.User;
import com.kt.dto.order.OrderRequest;
import com.kt.dto.order.OrderResponse;
import com.kt.repository.order.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private UserOrderService userOrderService;

    @Test
    @DisplayName("주문 상세 조회가 정상적으로 매핑된다")
    void 주문_상세조회_정상_매핑() {
        // given
        Long userId = 1L;
        Long orderId = 10L;

        User user = createUser();
        Receiver receiver = createReceiver("홍길동", "서울시 어딘가 1-1", "010-0000-0000");
        Order order = createOrder(receiver, user, OrderStatus.PENDING);

        Product product1 = createProduct("상품1", 1_000L, 10L, "상품 상세설명");
        Product product2 = createProduct("상품2", 2_000L, 5L, "상품 상세설명");
        createOrderProduct(order, product1, 2L);
        createOrderProduct(order, product2, 1L);
        long expectedTotalPrice = 1_000L * 2 + 2_000L * 1;

        given(orderRepository.findByIdAndUserIdOrThrow(orderId, userId, ErrorCode.NOT_FOUND_ORDER))
            .willReturn(order);

        // when
        OrderResponse.Detail detail = userOrderService.getByIdForUser(userId, orderId);

        // then
        assertThat(detail.receiverName()).isEqualTo(receiver.getName());
        assertThat(detail.receiverAddress()).isEqualTo(receiver.getAddress());
        assertThat(detail.receiverMobile()).isEqualTo(receiver.getMobile());

        assertThat(detail.items()).hasSize(2);

        assertThat(detail.totalPrice()).isEqualTo(expectedTotalPrice);

        assertThat(detail.status()).isEqualTo(order.getStatus());
        assertThat(detail.createdAt()).isEqualTo(order.getCreatedAt());

        then(orderRepository).should()
            .findByIdAndUserIdOrThrow(orderId, userId, ErrorCode.NOT_FOUND_ORDER);
    }

    @Test
    @DisplayName("주문 목록 조회 시 Summary DTO로 정상 매핑된다")
    void 주문목록_조회_정상_매핑() {
        // given
        Long userId = 1L;
        var pageable = PageRequest.of(0, 10);

        User user = createUser();
        Receiver receiver1 = createReceiver("홍길동", "서울시 1", "010-0000-0000");
        Receiver receiver2 = createReceiver("박동길", "서울시 2", "010-1111-2222");

        Order order1 = createOrder(receiver1, user, OrderStatus.PENDING);
        Order order2 = createOrder(receiver2, user, OrderStatus.COMPLETED);

        // order1: 상품 2개
        Product o1Product1 = createProduct("주문1-상품1", 1_000L, 10L, "상품 상세설명");
        Product o1Product2 = createProduct("주문1-상품2", 2_000L, 5L, "상품 상세설명");
        createOrderProduct(order1, o1Product1, 2L);
        createOrderProduct(order1, o1Product2, 1L);

        // order2: 상품 1개
        Product o2Product1 = createProduct("주문2-상품1", 3_000L, 3L, "상품 상세설명");
        createOrderProduct(order2, o2Product1, 1L);

        Page<Order> page = new PageImpl<>(List.of(order1, order2), pageable, 2);

        given(orderRepository.findAllByUserId(userId, pageable))
            .willReturn(page);

        // when
        Page<OrderResponse.Summary> result = userOrderService.listMyOrders(userId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        OrderResponse.Summary firstSummary = result.getContent().get(0);

        long expectedTotalPrice = 1_000L * 2 + 2_000L * 1;
        String expectedFirstProductName = "주문1-상품1";
        int expectedProductCount = 2;

        assertThat(firstSummary.totalPrice()).isEqualTo(expectedTotalPrice);
        assertThat(firstSummary.firstProductName()).isEqualTo(expectedFirstProductName);
        assertThat(firstSummary.productCount()).isEqualTo(expectedProductCount);
        assertThat(firstSummary.status()).isEqualTo(order1.getStatus());
        assertThat(firstSummary.createdAt()).isEqualTo(order1.getCreatedAt());

        then(orderRepository).should().findAllByUserId(userId, pageable);
    }

    @Test
    @DisplayName("주문이 수정 가능한 상태일 때 수령인 정보가 변경된다")
    void 주문수정_가능상태_수령인_변경() {
        // given
        Long userId = 1L;
        Long orderId = 10L;

        User user = createUser();
        Receiver receiver = createReceiver("홍길동", "서울시 1", "010-0000-0000");
        Order order = createOrder(receiver, user, OrderStatus.PENDING); // canUpdate() = true

        given(orderRepository.findByIdAndUserIdOrThrow(orderId, userId, ErrorCode.NOT_FOUND_ORDER))
            .willReturn(order);

        OrderRequest.Update request = new OrderRequest.Update(
            "새 수령인",
            "서울시 새 주소 123",
            "010-9999-8888"
        );

        // when
        userOrderService.updateOrder(userId, orderId, request);

        // then
        then(orderRepository).should()
            .findByIdAndUserIdOrThrow(orderId, userId, ErrorCode.NOT_FOUND_ORDER);

        assertThat(order.getReceiver().getName()).isEqualTo(request.receiverName());
        assertThat(order.getReceiver().getAddress()).isEqualTo(request.receiverAddress());
        assertThat(order.getReceiver().getMobile()).isEqualTo(request.receiverMobile());
    }

    @Test
    @DisplayName("주문이 수정 불가능한 상태일 경우 예외가 발생하고 수령인 정보는 변경되지 않는다")
    void 주문수정_불가상태_예외발생_및_수령인_유지() {
        // given
        Long userId = 1L;
        Long orderId = 10L;

        User user = createUser();
        Receiver receiver = createReceiver("홍길동", "서울시 1", "010-0000-0000");
        Order order = createOrder(receiver, user, OrderStatus.CANCELLED); // canUpdate() = false

        String originalName = order.getReceiver().getName();
        String originalAddress = order.getReceiver().getAddress();
        String originalMobile = order.getReceiver().getMobile();

        given(orderRepository.findByIdAndUserIdOrThrow(orderId, userId, ErrorCode.NOT_FOUND_ORDER))
            .willReturn(order);

        OrderRequest.Update request = new OrderRequest.Update(
            "새 수령인",
            "서울시 새 주소 123",
            "010-9999-8888"
        );

        // when & then
        assertThatThrownBy(() -> userOrderService.updateOrder(userId, orderId, request))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CANNOT_UPDATE_ORDER);

        // 수령인 정보가 변경되지 않았는지 검증
        assertThat(order.getReceiver().getName()).isEqualTo(originalName);
        assertThat(order.getReceiver().getAddress()).isEqualTo(originalAddress);
        assertThat(order.getReceiver().getMobile()).isEqualTo(originalMobile);

        then(orderRepository).should()
            .findByIdAndUserIdOrThrow(orderId, userId, ErrorCode.NOT_FOUND_ORDER);
    }

    // 픽스처 메서드
    private User createUser() {
        return User.customer(
            "test_user",
            "password123",
            "테스트 사용자",
            "test@test.com",
            "010-1234-5678",
            Gender.MALE,
            LocalDate.of(2025, 1, 1),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    private Receiver createReceiver(String name, String address, String mobile) {
        return new Receiver(name, address, mobile);
    }

    private Product createProduct(String name, Long price, Long stock, String description) {
        return new Product(name, price, stock, description);
    }

    private Order createOrder(Receiver receiver, User user, OrderStatus status) {
        Order order = Order.create(receiver, user);
        order.changeStatus(status);
        return order;
    }

    private OrderProduct createOrderProduct(Order order, Product product, Long quantity) {
        OrderProduct op = new OrderProduct(order, product, quantity);
        order.mapToOrderProduct(op);
        return op;
    }
}