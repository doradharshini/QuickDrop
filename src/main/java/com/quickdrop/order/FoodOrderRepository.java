package com.quickdrop.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

	List<FoodOrder> findByStatus(OrderStatus status);
}
