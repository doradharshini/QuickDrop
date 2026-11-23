package com.quickdrop.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateOrderRequest(
		@NotBlank String customerName,
		@NotBlank String restaurantName,
		@NotBlank String deliveryAddress,
		@NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount) {
}
