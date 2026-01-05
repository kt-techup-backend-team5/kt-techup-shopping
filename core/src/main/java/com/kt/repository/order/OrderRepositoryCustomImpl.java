package com.kt.repository.order;

import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.order.QOrder;
import com.kt.domain.orderproduct.QOrderProduct;
import com.kt.domain.product.QProduct;
import com.kt.domain.user.QUser;
import com.kt.dto.order.OrderResponse;
import com.kt.dto.order.OrderSearchCondition;
import com.kt.dto.order.QOrderResponse_Search;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {
	private final JPAQueryFactory jpaQueryFactory;
	private final QOrder order = QOrder.order;
	private final QOrderProduct orderProduct = QOrderProduct.orderProduct;
	private final QProduct product = QProduct.product;
	private final QUser user = QUser.user;

	@Override
	public Page<OrderResponse.Search> search(
			String keyword,
			Pageable pageable
	) {
		var booleanBuilder = new BooleanBuilder();

		booleanBuilder.and(containsProductName(keyword));

		var content = jpaQueryFactory
				.select(new QOrderResponse_Search(
						order.id,
						order.receiver.name,
						user.name,
						product.name,
						orderProduct.quantity,
						product.price.multiply(orderProduct.quantity),
						order.status,
						order.createdAt
				))
				.from(order)
				.join(order.user, user)
				.join(orderProduct).on(orderProduct.order.id.eq(order.id))
				.join(product).on(orderProduct.product.id.eq(product.id))
				.where(booleanBuilder)
				.orderBy(order.id.desc())
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		var total = (long)jpaQueryFactory.select(order.id)
				.from(order)
				.join(orderProduct).on(orderProduct.order.id.eq(order.id))
				.join(product).on(orderProduct.product.id.eq(product.id))
				.where(booleanBuilder)
				.fetch().size();

		return new PageImpl<>(content, pageable, total);
	}

	@Override
	public Page<Order> findByConditions(OrderSearchCondition condition, Pageable pageable) {
		List<Order> content = jpaQueryFactory
				.selectFrom(order)
				.join(order.user, user).fetchJoin()
				.where(
						eqUsername(condition.username()),
						eqReceiverName(condition.receiverName()),
						eqStatus(condition.status())
				)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.orderBy(order.id.desc())
				.fetch();

		JPAQuery<Long> countQuery = jpaQueryFactory
				.select(order.count())
				.from(order)
				.where(
						eqUsername(condition.username()),
						eqReceiverName(condition.receiverName()),
						eqStatus(condition.status())
				);
		
		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	private BooleanExpression containsProductName(String keyword) {
		return Strings.isNotBlank(keyword) ? product.name.containsIgnoreCase(keyword) : null;
	}

	private BooleanExpression eqUsername(String username) {
		return Strings.isNotBlank(username) ? user.name.eq(username) : null;
	}

	private BooleanExpression eqReceiverName(String receiverName) {
		return Strings.isNotBlank(receiverName) ? order.receiver.name.eq(receiverName) : null;
	}

	private BooleanExpression eqStatus(OrderStatus status) {
		return status != null ? order.status.eq(status) : null;
	}
}