package com.demo.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotNull Long bookId, @NotNull @Min(1) Integer quantity) {}