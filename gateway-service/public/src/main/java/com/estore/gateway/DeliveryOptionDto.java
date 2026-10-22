package com.estore.gateway;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryOptionDto {
    private String method;
    private MoneyDto cost;
}
