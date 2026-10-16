package com.estore.product.product;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_options")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "position")
    private int position;

    @ElementCollection
    @CollectionTable(name = "product_option_values", joinColumns = @JoinColumn(name = "option_id"))
    @Column(name = "value")
    @Builder.Default
    private List<String> values = new ArrayList<>();
}
