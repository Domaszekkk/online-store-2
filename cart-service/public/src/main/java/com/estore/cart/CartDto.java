package com.estore.cart;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartDto {
    private UUID id;
    private String status;
    private List<CartItemDto> items;
    private MoneyDto total;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
}
