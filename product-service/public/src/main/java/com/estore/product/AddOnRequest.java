package com.estore.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOnRequest {
    @NotNull
    private UUID productId;

    @NotBlank
    private String name;

    @NotNull
    @Valid
    private MoneyRequest price;
}
