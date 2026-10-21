package com.estore.invoice.document;

import com.estore.invoice.Document;
import com.estore.invoice.DocumentDto;
import com.estore.invoice.DocumentService;
import com.estore.invoice.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Invoices and purchase confirmations issued for placed orders")
public class DocumentController {
    private final DocumentService documentService;
    private final DocumentMapper documentMapper;

    @Operation(summary = "Download the document of an order as PDF",
            description = "Returns the invoice (when the customer asked for one) or the purchase confirmation of the order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF file"),
            @ApiResponse(responseCode = "404", description = "No document issued for this order (yet)")
    })
    @GetMapping(value = "/order/{orderId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdf(@Parameter(description = "Id of the order") @PathVariable UUID orderId) {
        Document document = documentService.getDocumentByOrderId(orderId);
        byte[] pdf = documentService.getPdfByOrderId(orderId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(fileName(document))
                        .build()
                        .toString())
                .body(pdf);
    }

    @Operation(summary = "Get the document of an order", description = "Returns type, number, lines and totals as JSON")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document found"),
            @ApiResponse(responseCode = "404", description = "No document issued for this order (yet)")
    })
    @GetMapping(value = "/order/{orderId}/details", produces = MediaType.APPLICATION_JSON_VALUE)
    public DocumentDto getDetails(@Parameter(description = "Id of the order") @PathVariable UUID orderId) {
        return documentMapper.toDto(documentService.getDocumentByOrderId(orderId));
    }

    private static String fileName(Document document) {
        String prefix = document.getType() == DocumentType.INVOICE ? "faktura" : "potwierdzenie-zakupu";
        return prefix + "-" + document.getNumber().replace('/', '-') + ".pdf";
    }
}
