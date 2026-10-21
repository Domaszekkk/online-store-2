package com.estore.invoice.document;

import com.estore.invoice.DocumentNumberPort;
import com.estore.invoice.DocumentRepositoryPort;
import com.estore.invoice.DocumentService;
import com.estore.invoice.PdfRendererPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DocumentServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public DocumentService documentService(DocumentRepositoryPort documentRepositoryPort,
                                           DocumentNumberPort documentNumberPort,
                                           PdfRendererPort pdfRendererPort,
                                           SellerProperties sellerProperties,
                                           Clock clock) {
        return new DocumentService(documentRepositoryPort, documentNumberPort, pdfRendererPort,
                sellerProperties.toSeller(), clock);
    }
}
