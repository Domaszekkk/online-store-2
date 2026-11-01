package com.estore.order;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartDetails {
    private UUID id;
    private long version;

    @Builder.Default
    private List<CartItemDetails> items = new ArrayList<>();
}
