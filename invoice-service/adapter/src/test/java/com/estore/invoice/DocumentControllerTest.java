package com.estore.invoice;

import com.estore.invoice.document.DocumentController;
import com.estore.invoice.document.DocumentMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(DocumentMapperImpl.class)
class DocumentControllerTest {
    private static final byte[] PDF = {37, 80, 68, 70, 45};

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private DocumentService documentService;

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    private Document invoice(UUID orderId) {
        return Document.builder()
                .id(UUID.randomUUID())
                .type(DocumentType.INVOICE)
                .number("FV/2026/10/000001")
                .orderId(orderId)
                .orderNumber("ORD-2026-000001")
                .issuedAt(Instant.parse("2026-10-16T10:00:00Z"))
                .saleDate(Instant.parse("2026-10-16T09:59:00Z"))
                .paymentMethod("BLIK")
                .deliveryMethod("COURIER")
                .seller(Seller.builder().name("E-Store Sp. z o.o.").taxIdentificationNumber("1234563218").build())
                .buyer(Buyer.builder().fullName("Jan Kowalski").companyName("Kowalski Sp. z o.o.")
                        .taxIdentificationNumber("5260250274").build())
                .lines(List.of(DocumentLine.of(1, "ProBook X15", "PBX15-I7-16", 2, pln("4599.00"), VatRate.VAT_23)))
                .vatSummaries(List.of(VatSummary.fromGross(VatRate.VAT_23, pln("9198.00"))))
                .netTotal(pln("7478.05"))
                .vatTotal(pln("1719.95"))
                .grossTotal(pln("9198.00"))
                .build();
    }

    @Test
    void getPdf_DocumentExists_ReturnsPdfInline() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(documentService.getDocumentByOrderId(orderId)).thenReturn(invoice(orderId));
        when(documentService.getPdfByOrderId(orderId)).thenReturn(PDF);

        mockMvc.perform(get("/documents/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("inline; filename=\"faktura-FV-2026-10-000001.pdf\"")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void getPdf_NoDocument_Returns404WithMessage() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(documentService.getDocumentByOrderId(orderId)).thenThrow(new DocumentNotFoundException(orderId));

        mockMvc.perform(get("/documents/order/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No document has been issued for order " + orderId));
    }

    @Test
    void getDetails_DocumentExists_ReturnsTypeNumberAndTotals() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(documentService.getDocumentByOrderId(orderId)).thenReturn(invoice(orderId));

        mockMvc.perform(get("/documents/order/{orderId}/details", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INVOICE"))
                .andExpect(jsonPath("$.number").value("FV/2026/10/000001"))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.buyer.taxIdentificationNumber").value("5260250274"))
                .andExpect(jsonPath("$.lines[0].vatRate").value("VAT_23"))
                .andExpect(jsonPath("$.vatSummaries[0].netTotal.amount").value(7478.05))
                .andExpect(jsonPath("$.grossTotal.amount").value(9198.00))
                .andExpect(jsonPath("$.grossTotal.currency").value("PLN"));
    }

    @Test
    void getDetails_NoDocument_Returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(documentService.getDocumentByOrderId(orderId)).thenThrow(new DocumentNotFoundException(orderId));

        mockMvc.perform(get("/documents/order/{orderId}/details", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDetails_InvalidOrderId_Returns400() throws Exception {
        mockMvc.perform(get("/documents/order/{orderId}/details", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value of parameter orderId: not-a-uuid"));
    }
}
