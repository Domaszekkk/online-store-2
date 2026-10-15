package com.estore.product;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MoneyRequest {
    @NotNull
    @PositiveOrZero
    @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;

    @NotNull
    @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 code, e.g. PLN")
    private String currency;
}
