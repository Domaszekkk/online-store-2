package com.estore.product;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductOption {
    private String name;
    private int position;

    @Builder.Default
    private List<String> values = new ArrayList<>();
}
