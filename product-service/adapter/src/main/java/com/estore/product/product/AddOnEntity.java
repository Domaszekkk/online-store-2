package com.estore.product.product;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "add_ons")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_ref_id")
    private UUID productId;
}
