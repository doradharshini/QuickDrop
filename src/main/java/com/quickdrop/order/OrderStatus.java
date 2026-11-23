package com.quickdrop.order;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle. Each status declares which statuses it may move to next,
 * so the state machine lives in one place.
 */
public enum OrderStatus {
	DELIVERED,
	CANCELLED,
	PICKED_UP,
	READY,
	PREPARING,
	ACCEPTED,
	PLACED;

	private Set<OrderStatus> next;

	static {
		PLACED.next = EnumSet.of(ACCEPTED, CANCELLED);
		ACCEPTED.next = EnumSet.of(PREPARING, CANCELLED);
		PREPARING.next = EnumSet.of(READY, CANCELLED);
		READY.next = EnumSet.of(PICKED_UP);
		PICKED_UP.next = EnumSet.of(DELIVERED);
		DELIVERED.next = EnumSet.noneOf(OrderStatus.class);
		CANCELLED.next = EnumSet.noneOf(OrderStatus.class);
	}

	public Set<OrderStatus> allowedNext() {
		return Set.copyOf(next);
	}

	public boolean canTransitionTo(OrderStatus target) {
		return next.contains(target);
	}

	public boolean isTerminal() {
		return next.isEmpty();
	}
}
