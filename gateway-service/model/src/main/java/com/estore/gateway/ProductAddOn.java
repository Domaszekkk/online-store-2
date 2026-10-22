package com.estore.gateway;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductAddOn {
    private UUID productId;
    private String name;
    private Money price;
}
