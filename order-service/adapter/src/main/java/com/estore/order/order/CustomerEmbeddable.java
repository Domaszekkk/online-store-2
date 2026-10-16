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
public class CustomerEmbeddable {

    @Column(name = "customer_first_name", length = 100)
    private String firstName;

    @Column(name = "customer_last_name", length = 100)
    private String lastName;

    @Column(name = "customer_email")
    private String email;

    @Column(name = "customer_phone", length = 20)
    private String phone;
}
