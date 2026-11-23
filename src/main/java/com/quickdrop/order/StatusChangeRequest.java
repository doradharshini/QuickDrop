package com.quickdrop.order;

import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(@NotNull OrderStatus status) {
}
