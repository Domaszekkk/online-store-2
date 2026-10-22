package com.estore.gateway;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
