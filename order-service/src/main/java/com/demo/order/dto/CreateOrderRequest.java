package com.demo.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotNull Long bookId, @NotNull @Min(1) Integer quantity) {}