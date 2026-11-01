package com.estore.gateway;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Cart {
    private UUID id;
    private String status;
    private List<CartItem> items;
    private Money total;
}
