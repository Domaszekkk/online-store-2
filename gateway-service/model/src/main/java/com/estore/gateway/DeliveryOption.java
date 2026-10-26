package com.estore.gateway;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryOption {
    private String method;
    private Money cost;
}
