package com.estore.cart.cart;

import com.estore.cart.VatRate;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "cart_item_addons")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemAddOnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_ref_id")
    private UUID productId;

    @Column(name = "name")
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3))
    })
    private MoneyEmbeddable price;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_rate", length = 20)
    private VatRate vatRate;

    @Column(name = "quantity")
    private int quantity;
}
