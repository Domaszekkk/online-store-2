package com.estore.gateway.document;

import com.estore.gateway.client.RemoteServiceClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "invoice-service", url = "${invoice.service.url}", configuration = RemoteServiceClientConfiguration.class)
public interface DocumentClient {

    @GetMapping("/documents/order/{orderId}")
    ResponseEntity<byte[]> getDocument(@PathVariable("orderId") UUID orderId);
}
