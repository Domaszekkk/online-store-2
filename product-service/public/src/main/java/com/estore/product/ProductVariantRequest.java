package com.estore.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariantRequest {
    /** Optional: send the existing id on update to keep it (carts and orders reference it). */
    private UUID id;

    @NotBlank
    private String stockKeepingUnit;

    private Map<String, String> optionValues;

    @NotNull
    @Valid
    private MoneyRequest price;

    @NotNull
    private Availability availability;
}
