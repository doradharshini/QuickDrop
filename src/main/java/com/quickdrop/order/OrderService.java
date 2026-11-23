package com.quickdrop.order;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private final FoodOrderRepository repository;

	public OrderService(FoodOrderRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public FoodOrder create(CreateOrderRequest request) {
		FoodOrder order = new FoodOrder();
		order.setCustomerName(request.customerName());
		order.setRestaurantName(request.restaurantName());
		order.setDeliveryAddress(request.deliveryAddress());
		order.setTotalAmount(request.totalAmount());
		return repository.save(order);
	}

	@Transactional(readOnly = true)
	public FoodOrder get(Long id) {
		return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public List<FoodOrder> list(OrderStatus status) {
		return status == null ? repository.findAll() : repository.findByStatus(status);
	}

	/** Moves an order to {@code target}, rejecting any transition the state machine does not allow. */
	@Transactional
	public FoodOrder transition(Long id, OrderStatus target) {
		FoodOrder order = get(id);
		if (!order.getStatus().canTransitionTo(target)) {
			throw new IllegalOrderTransitionException(order.getStatus(), target);
		}
		order.setStatus(target);
		return repository.save(order);
	}
}
