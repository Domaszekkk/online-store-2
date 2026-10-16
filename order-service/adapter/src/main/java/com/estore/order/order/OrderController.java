package com.estore.order.order;

import com.estore.order.OrderDto;
import com.estore.order.OrderService;
import com.estore.order.PlaceOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for placing orders and browsing order history")
public class OrderController {
    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @Operation(summary = "Place an order",
            description = "Checks out the cart in cart-service and creates an order from its content (prices are snapshotted)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order placed"),
            @ApiResponse(responseCode = "400", description = "Invalid request (missing customer data, address, payment or delivery method)"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "409", description = "Cart is empty or an order was already placed from it"),
            @ApiResponse(responseCode = "503", description = "Cart service is unavailable")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return orderMapper.toDto(orderService.placeOrder(orderMapper.toCommand(request)));
    }

    @Operation(summary = "Get order by id", description = "Returns a single order identified by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public OrderDto getOrderById(@Parameter(description = "Id of the order to retrieve") @PathVariable UUID id) {
        return orderMapper.toDto(orderService.getOrderById(id));
    }

    @Operation(summary = "Get order history",
            description = "Returns orders newest first; with email only the orders of that customer (case-insensitive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders returned (possibly empty list)")
    })
    @GetMapping
    public List<OrderDto> getOrders(@Parameter(description = "Customer email to filter by (optional)")
                                    @RequestParam(required = false) String email) {
        return orderMapper.toDtos(orderService.getOrders(email));
    }
}
