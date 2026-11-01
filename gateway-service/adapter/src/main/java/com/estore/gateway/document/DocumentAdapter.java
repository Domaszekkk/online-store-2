package com.estore.gateway.document;

import com.estore.gateway.DocumentPort;
import com.estore.gateway.OrderDocument;
import com.estore.gateway.client.RemoteServiceCalls;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentAdapter implements DocumentPort {
    private static final String SERVICE_NAME = "invoice-service";

    private final DocumentClient documentClient;
    private final RemoteServiceCalls remoteServiceCalls;

    @Override
    public Optional<OrderDocument> findByOrderId(UUID orderId) {
        try {
            ResponseEntity<byte[]> response = documentClient.getDocument(orderId);
            return Optional.of(OrderDocument.builder()
                    .fileName(fileName(response, orderId))
                    .content(response.getBody())
                    .build());
        } catch (FeignException.NotFound exception) {
            return Optional.empty();
        } catch (FeignException exception) {
            throw remoteServiceCalls.translate(SERVICE_NAME, exception);
        }
    }

    private static String fileName(ResponseEntity<byte[]> response, UUID orderId) {
        String fileName = response.getHeaders().getContentDisposition().getFilename();
        return fileName != null ? fileName : "document-" + orderId + ".pdf";
    }
}
