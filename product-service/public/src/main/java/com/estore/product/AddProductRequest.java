package com.estore.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddProductRequest {
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

    private List<@Valid @NotNull ProductOptionRequest> options;

    private List<@Valid @NotNull ProductVariantRequest> variants;

    private List<@Valid @NotNull AddOnRequest> addOns;
}
