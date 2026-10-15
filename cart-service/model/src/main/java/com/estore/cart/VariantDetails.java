package com.estore.cart;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VariantDetails {
    private UUID id;
    private String stockKeepingUnit;
    private Money price;
    private String availability;

    public boolean isAvailable() {
        return "AVAILABLE".equals(availability);
    }
}
