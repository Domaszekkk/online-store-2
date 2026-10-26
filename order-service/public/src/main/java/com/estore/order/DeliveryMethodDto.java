package com.estore.order;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryMethodDto {
    private String method;
    private MoneyDto cost;
}
