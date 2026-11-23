package com.quickdrop.order;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService service;

	public OrderController(OrderService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public FoodOrder create(@Valid @RequestBody CreateOrderRequest request) {
		return service.create(request);
	}

	@GetMapping
	public List<FoodOrder> list(@RequestParam(required = false) OrderStatus status) {
		return service.list(status);
	}

	@GetMapping("/{id}")
	public FoodOrder get(@PathVariable Long id) {
		return service.get(id);
	}

	@PostMapping("/{id}/status")
	public FoodOrder changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
		return service.transition(id, request.status());
	}
}
