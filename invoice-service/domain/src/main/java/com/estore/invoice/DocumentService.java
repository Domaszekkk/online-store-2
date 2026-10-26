package com.estore.invoice;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

@RequiredArgsConstructor
public class DocumentService {
    private static final ZoneId STORE_ZONE = ZoneId.of("Europe/Warsaw");

    private final DocumentRepositoryPort documentRepositoryPort;
    private final DocumentNumberPort documentNumberPort;
    private final PdfRendererPort pdfRendererPort;
    private final Seller seller;
    private final Clock clock;

    public Document issue(IssueDocumentCommand command) {
        return documentRepositoryPort.findByOrderId(command.getOrderId())
                .orElseGet(() -> issueNew(command));
    }

    public Document getDocumentByOrderId(UUID orderId) {
        return documentRepositoryPort.findByOrderId(orderId)
                .orElseThrow(() -> new DocumentNotFoundException(orderId));
    }

    public byte[] getPdfByOrderId(UUID orderId) {
        return documentRepositoryPort.findPdfByOrderId(orderId)
                .orElseThrow(() -> new DocumentNotFoundException(orderId));
    }

    private Document issueNew(IssueDocumentCommand command) {
        Instant issuedAt = clock.instant();
        DocumentType type = Document.typeFor(command);
        YearMonth period = YearMonth.from(issuedAt.atZone(STORE_ZONE));
        String number = type.formatNumber(period, documentNumberPort.nextSequenceNumber(type, period));
        Document document = Document.issue(command, seller, number, issuedAt);
        return documentRepositoryPort.save(document, pdfRendererPort.render(document));
    }
}
