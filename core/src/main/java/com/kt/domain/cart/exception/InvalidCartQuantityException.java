package com.kt.domain.cart.exception;

import com.kt.domain.common.exception.DomainException;

public class InvalidCartQuantityException extends DomainException {
    public InvalidCartQuantityException(Long quantity) {
        super("장바구니 수량은 1 이상이어야 합니다. quantity=" + quantity);
    }
}