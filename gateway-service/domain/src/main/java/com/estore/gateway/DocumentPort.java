package com.estore.gateway;

import java.util.Optional;
import java.util.UUID;

public interface DocumentPort {
    Optional<OrderDocument> findByOrderId(UUID orderId);
}
