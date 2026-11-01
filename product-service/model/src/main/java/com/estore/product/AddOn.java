package com.estore.product;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOn {
    private UUID productId;
    private String name;
    private Money price;
    private VatRate vatRate;
    private Availability availability;
}
