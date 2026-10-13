package com.estore.product;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductOptionDto {
    private String name;
    private int position;
    private List<String> values;
}
