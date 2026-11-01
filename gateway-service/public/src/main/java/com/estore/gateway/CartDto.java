package com.estore.gateway;

import lombok.*;

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
}
