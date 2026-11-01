package com.estore.gateway.order;

import com.estore.gateway.DocumentPendingMessage;
import com.estore.gateway.OrderDocument;
import com.estore.gateway.OrderDto;
import com.estore.gateway.OrderPlacement;
import com.estore.gateway.OrderPreviewDto;
import com.estore.gateway.PlaceOrderRequest;
import com.estore.gateway.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/shop/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Placing an order and its summary for the shop frontend")
public class OrderController {
    private final ShopService shopService;
    private final OrderMapper orderMapper;

    @Value("${gateway.document.retry-after-seconds:2}")
    private int documentRetryAfterSeconds;

    @Operation(summary = "Preview an order",
            description = "Screen 3: returns the order summary (products with prices, delivery cost, totals and the entered data) "
                    + "without placing the order; the cart stays open")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order summary returned"),
            @ApiResponse(responseCode = "400", description = "Invalid order data"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "409", description = "Cart is empty or an order was already placed from it"),
            @ApiResponse(responseCode = "503", description = "Order service is unavailable")
    })
    @PostMapping("/preview")
    public OrderPreviewDto previewOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return orderMapper.toDto(shopService.previewOrder(orderMapper.toCommand(request)));
    }

    @Operation(summary = "Place an order",
            description = "Places an order from the cart; billingAddress means an invoice, with companyName and taxIdentificationNumber a company invoice. "
                    + "cartVersion is taken from the order preview (screen 3), so the order matches the summary the customer has seen")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order placed"),
            @ApiResponse(responseCode = "400", description = "Invalid order data or missing cart version"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "409", description = "Cart is empty, has changed since the preview, contains unavailable products "
                    + "or an order was already placed from it"),
            @ApiResponse(responseCode = "503", description = "Order service is unavailable")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto placeOrder(@Validated({Default.class, OrderPlacement.class}) @RequestBody PlaceOrderRequest request) {
        return orderMapper.toDto(shopService.placeOrder(orderMapper.toCommand(request)));
    }

    @Operation(summary = "Get order", description = "Returns the order summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "503", description = "Order service is unavailable")
    })
    @GetMapping("/{id}")
    public OrderDto getOrder(@Parameter(description = "Id of the order") @PathVariable UUID id) {
        return orderMapper.toDto(shopService.getOrder(id));
    }

    @Operation(summary = "Download the document of an order",
            description = "Returns the invoice or the purchase confirmation as PDF; it is issued a few seconds after the order is placed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF file"),
            @ApiResponse(responseCode = "202", description = "Document is being prepared, try again after Retry-After seconds"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "503", description = "Order or invoice service is unavailable")
    })
    @GetMapping("/{id}/document")
    public ResponseEntity<?> getOrderDocument(@Parameter(description = "Id of the order") @PathVariable UUID id) {
        Optional<OrderDocument> document = shopService.findOrderDocument(id);
        if (document.isEmpty()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(documentRetryAfterSeconds))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DocumentPendingMessage("Document of order " + id + " is being prepared, try again in a few seconds"));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(document.get().getFileName())
                        .build()
                        .toString())
                .body(document.get().getContent());
    }
}
