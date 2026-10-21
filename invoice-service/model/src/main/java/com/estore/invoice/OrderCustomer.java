package com.estore.invoice;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCustomer {
    private String firstName;
    private String lastName;
    private String email;
}
