package com.quickdrop.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "food_order")
public class FoodOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String customerName;

	@Column(nullable = false)
	private String restaurantName;

	@Column(nullable = false)
	private String deliveryAddress;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status = OrderStatus.PLACED;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() { return id; }
	public String getCustomerName() { return customerName; }
	public void setCustomerName(String customerName) { this.customerName = customerName; }
	public String getRestaurantName() { return restaurantName; }
	public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
	public String getDeliveryAddress() { return deliveryAddress; }
	public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
	public BigDecimal getTotalAmount() { return totalAmount; }
	public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
	public OrderStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	/** Only {@link OrderService} should call this, after validating the transition. */
	void setStatus(OrderStatus status) { this.status = status; }
}
