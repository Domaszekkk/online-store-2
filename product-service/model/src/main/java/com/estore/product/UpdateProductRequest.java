package com.estore.product;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProductRequest {
    private String name;
    private ProductType type;
    private String description;
    private Money basePrice;

    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
}
