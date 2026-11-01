package com.estore.gateway.order;

import com.estore.gateway.CheckoutOptionsDto;
import com.estore.gateway.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shop/checkout-options")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Placing an order and its summary for the shop frontend")
public class CheckoutOptionsController {
    private final ShopService shopService;
    private final OrderMapper orderMapper;

    @Operation(summary = "Get checkout options", description = "Returns payment methods and delivery methods with their gross cost")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkout options returned"),
            @ApiResponse(responseCode = "503", description = "Order service is unavailable")
    })
    @GetMapping
    public CheckoutOptionsDto getCheckoutOptions() {
        return orderMapper.toDto(shopService.getCheckoutOptions());
    }
}
