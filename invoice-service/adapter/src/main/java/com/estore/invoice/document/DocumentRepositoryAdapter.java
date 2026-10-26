package com.estore.invoice.document;

import com.estore.invoice.Document;
import com.estore.invoice.DocumentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentRepositoryAdapter implements DocumentRepositoryPort {
    private final DocumentJpaRepository documentJpaRepository;
    private final DocumentPdfJpaRepository documentPdfJpaRepository;
    private final DocumentMapper documentMapper;

    @Override
    @Transactional
    public Document save(Document document, byte[] pdfContent) {
        DocumentEntity saved = documentJpaRepository.save(documentMapper.toEntity(document));
        documentPdfJpaRepository.save(DocumentPdfEntity.builder()
                .documentId(saved.getId())
                .orderId(saved.getOrderId())
                .content(pdfContent)
                .build());
        return documentMapper.toModel(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findByOrderId(UUID orderId) {
        return documentJpaRepository.findByOrderId(orderId).map(documentMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findPdfByOrderId(UUID orderId) {
        return documentPdfJpaRepository.findByOrderId(orderId).map(DocumentPdfEntity::getContent);
    }
}
