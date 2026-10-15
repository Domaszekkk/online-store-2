package com.estore.product.product;

import com.estore.product.Availability;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariantEntity {

    @Id
    private UUID id;

    @Column(name = "stock_keeping_unit")
    private String stockKeepingUnit;

    @ElementCollection
    @CollectionTable(name = "product_variant_option_values", joinColumns = @JoinColumn(name = "variant_id"))
    @MapKeyColumn(name = "option_name")
    @Column(name = "option_value")
    @Builder.Default
    private Map<String, String> optionValues = new HashMap<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3))
    })
    private MoneyEmbeddable price;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", length = 50)
    private Availability availability;
}
