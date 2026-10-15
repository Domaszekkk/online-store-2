package com.estore.cart.cart;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemEntity {

    @Id
    private UUID id;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "stock_keeping_unit")
    private String stockKeepingUnit;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount", precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency", length = 3))
    })
    private MoneyEmbeddable unitPrice;

    @Column(name = "quantity")
    private int quantity;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cart_item_id")
    @Builder.Default
    private List<CartItemAddOnEntity> addOns = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "line_total_amount", precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "line_total_currency", length = 3))
    })
    private MoneyEmbeddable lineTotal;
}
