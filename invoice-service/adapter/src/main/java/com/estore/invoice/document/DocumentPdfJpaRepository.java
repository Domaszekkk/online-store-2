package com.estore.invoice.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentPdfJpaRepository extends JpaRepository<DocumentPdfEntity, UUID> {

    Optional<DocumentPdfEntity> findByOrderId(UUID orderId);
}
