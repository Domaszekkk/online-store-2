package com.estore.gateway;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckoutOptions {
    private List<String> paymentMethods;
    private List<DeliveryOption> deliveryMethods;
}
