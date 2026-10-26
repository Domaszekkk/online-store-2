package com.estore.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductOptionRequest {
    @NotBlank
    private String name;

    @PositiveOrZero
    private int position;

    @NotEmpty
    private List<@NotBlank String> values;
}
