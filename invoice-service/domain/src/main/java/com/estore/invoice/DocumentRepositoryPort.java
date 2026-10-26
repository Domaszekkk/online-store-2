package com.estore.invoice;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {
    Document save(Document document, byte[] pdfContent);

    Optional<Document> findByOrderId(UUID orderId);

    Optional<byte[]> findPdfByOrderId(UUID orderId);
}
