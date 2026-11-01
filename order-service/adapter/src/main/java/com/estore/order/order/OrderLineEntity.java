package com.estore.order.order;

import com.estore.order.VatRate;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_lines")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLineEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_rate", length = 20)
    private VatRate vatRate;

    @Column(name = "quantity")
    private int quantity;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_line_id")
    @OrderBy("id ASC")
    @Builder.Default
    private List<OrderLineAddOnEntity> addOns = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "line_total_amount", precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "line_total_currency", length = 3))
    })
    private MoneyEmbeddable lineTotal;
}
