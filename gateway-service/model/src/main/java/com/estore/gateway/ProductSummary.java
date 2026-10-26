package com.estore.gateway;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSummary {
    private UUID id;
    private String name;
    private String type;
    private Money basePrice;
}
