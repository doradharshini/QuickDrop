package com.quickdrop.order;

public class IllegalOrderTransitionException extends RuntimeException {

	public IllegalOrderTransitionException(OrderStatus from, OrderStatus to) {
		super("Cannot move order from " + from + " to " + to + ". Allowed: " + from.allowedNext());
	}
}
