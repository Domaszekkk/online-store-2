package com.estore.cart;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOnDetails {
    private UUID productId;
    private String name;
    private Money price;
    private VatRate vatRate;
    private String availability;

    public boolean isAvailable() {
        return "AVAILABLE".equals(availability);
    }
}
