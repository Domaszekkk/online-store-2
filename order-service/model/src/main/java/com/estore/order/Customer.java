package com.estore.order;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Customer {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
