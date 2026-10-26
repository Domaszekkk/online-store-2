package com.estore.gateway;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductOptionDto {
    private String name;
    private List<String> values;
}
