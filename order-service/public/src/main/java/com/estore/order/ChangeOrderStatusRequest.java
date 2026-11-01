package com.estore.order;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeOrderStatusRequest {
    @NotNull
    private OrderStatus status;
}
