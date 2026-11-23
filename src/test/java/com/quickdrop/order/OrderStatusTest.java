package com.quickdrop.order;

import static com.quickdrop.order.OrderStatus.ACCEPTED;
import static com.quickdrop.order.OrderStatus.CANCELLED;
import static com.quickdrop.order.OrderStatus.DELIVERED;
import static com.quickdrop.order.OrderStatus.PICKED_UP;
import static com.quickdrop.order.OrderStatus.PLACED;
import static com.quickdrop.order.OrderStatus.PREPARING;
import static com.quickdrop.order.OrderStatus.READY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

	private static final Map<OrderStatus, Set<OrderStatus>> EXPECTED = Map.of(
			PLACED, Set.of(ACCEPTED, CANCELLED),
			ACCEPTED, Set.of(PREPARING, CANCELLED),
			PREPARING, Set.of(READY, CANCELLED),
			READY, Set.of(PICKED_UP),
			PICKED_UP, Set.of(DELIVERED),
			DELIVERED, Set.of(),
			CANCELLED, Set.of());

	@Test
	void everyStatusHasExactlyTheExpectedTransitions() {
		assertThat(EXPECTED).containsOnlyKeys(OrderStatus.values());
		for (OrderStatus from : OrderStatus.values()) {
			for (OrderStatus to : OrderStatus.values()) {
				assertThat(from.canTransitionTo(to))
						.as("%s -> %s", from, to)
						.isEqualTo(EXPECTED.get(from).contains(to));
			}
		}
	}

	@Test
	void deliveredAndCancelledAreTerminal() {
		assertThat(DELIVERED.isTerminal()).isTrue();
		assertThat(CANCELLED.isTerminal()).isTrue();
		assertThat(PLACED.isTerminal()).isFalse();
	}

	@Test
	void cannotCancelOnceFoodIsReady() {
		assertThat(READY.canTransitionTo(CANCELLED)).isFalse();
		assertThat(PICKED_UP.canTransitionTo(CANCELLED)).isFalse();
	}
}
