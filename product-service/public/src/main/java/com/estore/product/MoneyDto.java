package com.estore.product;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MoneyDto {
    private BigDecimal amount;
    private String currency;
}
