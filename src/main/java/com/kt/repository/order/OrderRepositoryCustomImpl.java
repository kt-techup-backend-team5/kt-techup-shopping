package com.kt.repository.order;

import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.kt.domain.order.QOrder;
import com.kt.domain.orderproduct.QOrderProduct;
import com.kt.domain.product.QProduct;
import com.kt.dto.order.OrderResponse;
import com.kt.dto.order.QOrderResponse_Search;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {
	// QClass를 임포트해서 쿼리를 작성할 때 사용하면 됩니다.
	private final JPAQueryFactory jpaQueryFactory;
	private final QOrder order = QOrder.order;
	private final QOrderProduct orderProduct = QOrderProduct.orderProduct;
	private final QProduct product = QProduct.product;

	@Override
	public Page<OrderResponse.Search> search(
		String keyword,
		Pageable pageable
	) {
		// 페이징을 구현할때
		// offset, limit
		// select *
		// booleanBuilder, BooleanExpression
		var booleanBuilder = new BooleanBuilder();

		booleanBuilder.and(containsProductName(keyword));

		// booleanBuilder.and()
		// booleanBuilder.or()
		// booleanBuilder안에다가 booleanExpression을 추가해주는 방식으로

		var content = jpaQueryFactory
			// order자리에 QOrderResponse_Search
			// totalPrice는 product의 price * orderProduct.quantity
			.select(new QOrderResponse_Search(
				order.id,
				order.receiver.name,
				product.name,
				orderProduct.quantity,
				product.price.multiply(orderProduct.quantity),
				order.status,
				order.createdAt
			))
			.from(order)
			.join(orderProduct).on(orderProduct.order.id.eq(order.id))
			.join(product).on(orderProduct.product.id.eq(product.id))
			.where(booleanBuilder)
			.orderBy(order.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		// 최초에 페이지 접근했을때 -> 전체검색이 되야할까 아니면 특정키워드검색이 자동으로 되야하나
		// name like '%null%' (동작 해야하나?) - 동작안해야
		// keyword = null

		var total = (long)jpaQueryFactory.select(order.id)
			.from(order)
			.join(orderProduct).on(orderProduct.order.id.eq(order.id))
			.join(product).on(orderProduct.product.id.eq(product.id))
			.where(booleanBuilder)
			.fetch().size();

		return new PageImpl<>(content, pageable, total);
	}

	// 시작하는 '%keyword'
	// 끝나는 'keyword%'
	// 포함하는 '%" "%'
	// 공백이면 어쩌징

	// Strings
	// Objects
	private BooleanExpression containsProductName(String keyword) {
		// if(Strings.isNotBlank(keyword)) {
		// 	return product.name.containsIgnoreCase(keyword);
		// } else {
		// 	return null;
		// }
		return Strings.isNotBlank(keyword) ? product.name.containsIgnoreCase(keyword) : null;
	}
}
