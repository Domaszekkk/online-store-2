package com.estore.product;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOnDto {
    private UUID productId;
    private String name;
    private MoneyDto price;
}
