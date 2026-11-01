package com.estore.gateway;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckoutOptionsDto {
    private List<String> paymentMethods;
    private List<DeliveryOptionDto> deliveryMethods;
}
