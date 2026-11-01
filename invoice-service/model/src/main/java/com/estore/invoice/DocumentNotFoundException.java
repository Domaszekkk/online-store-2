package com.estore.invoice;

import java.util.UUID;

public class DocumentNotFoundException extends DocumentException {
    public DocumentNotFoundException(UUID orderId) {
        super("No document has been issued for order " + orderId, 404);
    }
}
