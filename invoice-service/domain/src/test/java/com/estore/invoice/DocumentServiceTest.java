package com.estore.invoice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");
    private static final byte[] PDF = {37, 80, 68, 70};

    private DocumentRepositoryPort documentRepositoryPort;
    private DocumentNumberPort documentNumberPort;
    private PdfRendererPort pdfRendererPort;
    private DocumentService documentService;

    @BeforeEach
    void setup() {
        this.documentRepositoryPort = Mockito.mock(DocumentRepositoryPort.class);
        this.documentNumberPort = Mockito.mock(DocumentNumberPort.class);
        this.pdfRendererPort = Mockito.mock(PdfRendererPort.class);
        Seller seller = Seller.builder().name("E-Store Sp. z o.o.").taxIdentificationNumber("7010000000").build();
        this.documentService = new DocumentService(documentRepositoryPort, documentNumberPort, pdfRendererPort,
                seller, Clock.fixed(NOW, ZoneOffset.UTC));
        when(documentRepositoryPort.findByOrderId(any())).thenReturn(Optional.empty());
        when(documentNumberPort.nextSequenceNumber(any(), any())).thenReturn(7L);
        when(pdfRendererPort.render(any())).thenReturn(PDF);
        when(documentRepositoryPort.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    private static void assertAmount(String expected, Money actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.amount()),
                () -> "expected " + expected + " but was " + actual.amount());
    }

    private IssueDocumentCommand command(OrderBillingAddress billingAddress, String deliveryMethod, String deliveryCost) {
        return IssueDocumentCommand.builder()
                .orderId(UUID.randomUUID())
                .orderNumber("ORD-2026-000001")
                .placedAt(NOW.minusSeconds(5))
                .customer(OrderCustomer.builder().firstName("Jan").lastName("Kowalski").email("jan.kowalski@example.com").build())
                .billingAddress(billingAddress)
                .paymentMethod("BLIK")
                .deliveryMethod(deliveryMethod)
                .deliveryCost(pln(deliveryCost))
                .deliveryVatRate(VatRate.VAT_23)
                .lines(List.of(OrderedLine.builder()
                        .productName("ProBook X15").stockKeepingUnit("PBX15-I7-16")
                        .unitPrice(pln("4599.00")).vatRate(VatRate.VAT_23).quantity(2)
                        .addOns(List.of(OrderedAddOn.builder()
                                .name("Kabel HDMI 2m").price(pln("39.00")).vatRate(VatRate.VAT_8).quantity(1)
                                .build()))
                        .build()))
                .build();
    }

    private OrderBillingAddress companyBillingAddress() {
        return OrderBillingAddress.builder()
                .companyName("Kowalski Sp. z o.o.").taxIdentificationNumber("5260250274")
                .street("Firmowa 5").city("Kraków").postalCode("30-001").country("Polska").build();
    }

    private OrderBillingAddress personBillingAddress() {
        return OrderBillingAddress.builder()
                .street("Domowa 7").city("Gdańsk").postalCode("80-001").country("Polska").build();
    }

    @Test
    void issue_CompanyBillingAddress_IssuesInvoiceWithVatSummaryPerRate() {
        Document result = documentService.issue(command(companyBillingAddress(), "COURIER", "19.99"));

        VatSummary standardRate = result.getVatSummaries().stream()
                .filter(summary -> summary.getVatRate() == VatRate.VAT_23).findFirst().orElseThrow();
        VatSummary reducedRate = result.getVatSummaries().stream()
                .filter(summary -> summary.getVatRate() == VatRate.VAT_8).findFirst().orElseThrow();
        assertAll(
                () -> assertEquals(DocumentType.INVOICE, result.getType()),
                () -> assertEquals("FV/2026/10/000007", result.getNumber()),
                () -> assertEquals(NOW, result.getIssuedAt()),
                () -> assertTrue(result.getBuyer().isCompany()),
                () -> assertEquals("Kowalski Sp. z o.o.", result.getBuyer().getCompanyName()),
                () -> assertEquals("5260250274", result.getBuyer().getTaxIdentificationNumber()),
                () -> assertEquals(3, result.getLines().size()),
                () -> assertEquals("Kabel HDMI 2m", result.getLines().get(1).getName()),
                () -> assertEquals("Dostawa kurierem", result.getLines().get(2).getName()),
                () -> assertAmount("9198.00", result.getLines().get(0).getGrossTotal()),
                () -> assertAmount("7478.05", result.getLines().get(0).getNetTotal()),
                () -> assertAmount("1719.95", result.getLines().get(0).getVatTotal()),
                () -> assertAmount("9217.99", standardRate.getGrossTotal()),
                () -> assertAmount("7494.30", standardRate.getNetTotal()),
                () -> assertAmount("1723.69", standardRate.getVatTotal()),
                () -> assertAmount("39.00", reducedRate.getGrossTotal()),
                () -> assertAmount("36.11", reducedRate.getNetTotal()),
                () -> assertAmount("2.89", reducedRate.getVatTotal()),
                () -> assertAmount("7530.41", result.getNetTotal()),
                () -> assertAmount("1726.58", result.getVatTotal()),
                () -> assertAmount("9256.99", result.getGrossTotal())
        );
        verify(documentNumberPort).nextSequenceNumber(DocumentType.INVOICE, YearMonth.of(2026, 10));
        verify(documentRepositoryPort).save(result, PDF);
    }

    @Test
    void issue_PersonBillingAddress_IssuesInvoiceForPrivatePerson() {
        Document result = documentService.issue(command(personBillingAddress(), "COURIER", "19.99"));

        assertAll(
                () -> assertEquals(DocumentType.INVOICE, result.getType()),
                () -> assertFalse(result.getBuyer().isCompany()),
                () -> assertEquals("Jan Kowalski", result.getBuyer().getFullName()),
                () -> assertEquals("Domowa 7", result.getBuyer().getStreet())
        );
    }

    @Test
    void issue_NoBillingAddress_IssuesPurchaseConfirmationWithoutBuyer() {
        Document result = documentService.issue(command(null, "COURIER", "19.99"));

        assertAll(
                () -> assertEquals(DocumentType.PURCHASE_CONFIRMATION, result.getType()),
                () -> assertEquals("PZ/2026/10/000007", result.getNumber()),
                () -> assertNull(result.getBuyer()),
                () -> assertAmount("9256.99", result.getGrossTotal())
        );
        verify(documentNumberPort).nextSequenceNumber(DocumentType.PURCHASE_CONFIRMATION, YearMonth.of(2026, 10));
    }

    @Test
    void issue_Pickup_HasNoDeliveryLine() {
        Document result = documentService.issue(command(null, "PICKUP", "0.00"));

        assertAll(
                () -> assertEquals(2, result.getLines().size()),
                () -> assertAmount("9237.00", result.getGrossTotal())
        );
    }

    @Test
    void issue_DocumentAlreadyIssuedForOrder_ReturnsExistingWithoutNewNumberOrPdf() {
        IssueDocumentCommand command = command(companyBillingAddress(), "COURIER", "19.99");
        Document existing = Document.builder().orderId(command.getOrderId()).number("FV/2026/10/000001").build();
        when(documentRepositoryPort.findByOrderId(command.getOrderId())).thenReturn(Optional.of(existing));

        Document result = documentService.issue(command);

        assertSame(existing, result);
        verifyNoInteractions(documentNumberPort, pdfRendererPort);
        verify(documentRepositoryPort, never()).save(any(), any());
    }

    @Test
    void getPdfByOrderId_DocumentExists_ReturnsPdf() {
        UUID orderId = UUID.randomUUID();
        when(documentRepositoryPort.findPdfByOrderId(orderId)).thenReturn(Optional.of(PDF));

        assertArrayEquals(PDF, documentService.getPdfByOrderId(orderId));
    }

    @Test
    void getPdfByOrderId_NoDocument_ThrowsDocumentNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(documentRepositoryPort.findPdfByOrderId(eq(orderId))).thenReturn(Optional.empty());

        DocumentNotFoundException exception = Assertions.assertThrows(DocumentNotFoundException.class,
                () -> documentService.getPdfByOrderId(orderId));

        assertEquals(404, exception.getResponseCode());
    }

    @Test
    void getDocumentByOrderId_NoDocument_ThrowsDocumentNotFoundException() {
        UUID orderId = UUID.randomUUID();

        DocumentNotFoundException exception = Assertions.assertThrows(DocumentNotFoundException.class,
                () -> documentService.getDocumentByOrderId(orderId));

        assertEquals(404, exception.getResponseCode());
    }
}
