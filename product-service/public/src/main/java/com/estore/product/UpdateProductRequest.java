package com.estore.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Body of {@code PUT /products/{id}}. Collections left as {@code null} keep their current values;
 * a provided collection (also an empty one) replaces the existing one.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProductRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private ProductType type;

    @Size(max = 2000)
    private String description;

    @NotNull
    @Valid
    private MoneyRequest basePrice;

    private Map<String, String> attributes;

    @Valid
    private List<@NotNull ProductOptionRequest> options;

    @Valid
    private List<@NotNull ProductVariantRequest> variants;

    @Valid
    private List<@NotNull AddOnRequest> addOns;
}
