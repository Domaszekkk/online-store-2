package com.estore.order.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressEmbeddable {

    @Column(length = 255)
    private String street;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;
}
