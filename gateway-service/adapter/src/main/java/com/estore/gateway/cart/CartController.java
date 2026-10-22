package com.estore.gateway.cart;

import com.estore.gateway.AddItemRequest;
import com.estore.gateway.CartDto;
import com.estore.gateway.ChangeItemQuantityRequest;
import com.estore.gateway.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shop/carts")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart of the shop frontend")
public class CartController {
    private final ShopService shopService;
    private final CartMapper cartMapper;

    @Operation(summary = "Create a cart", description = "Creates a new empty cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cart created"),
            @ApiResponse(responseCode = "503", description = "Cart service is unavailable")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartDto createCart() {
        return cartMapper.toDto(shopService.createCart());
    }

    @Operation(summary = "Get cart", description = "Returns items, quantities, add-ons and the total of the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart found"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "503", description = "Cart service is unavailable")
    })
    @GetMapping("/{id}")
    public CartDto getCart(@Parameter(description = "Id of the cart") @PathVariable UUID id) {
        return cartMapper.toDto(shopService.getCart(id));
    }

    @Operation(summary = "Add item to cart", description = "Adds the chosen variant with optional add-ons to the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Cart or product not found"),
            @ApiResponse(responseCode = "409", description = "Variant unavailable or cart already checked out"),
            @ApiResponse(responseCode = "503", description = "Cart service is unavailable")
    })
    @PostMapping("/{id}/items")
    public CartDto addItem(@Parameter(description = "Id of the cart") @PathVariable UUID id,
                           @Valid @RequestBody AddItemRequest request) {
        return cartMapper.toDto(shopService.addItem(id, cartMapper.toCommand(request)));
    }

    @Operation(summary = "Change item quantity", description = "Sets a new quantity of the cart item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quantity changed"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found"),
            @ApiResponse(responseCode = "409", description = "Cart already checked out"),
            @ApiResponse(responseCode = "503", description = "Cart service is unavailable")
    })
    @PatchMapping("/{id}/items/{itemId}")
    public CartDto changeItemQuantity(@Parameter(description = "Id of the cart") @PathVariable UUID id,
                                      @Parameter(description = "Id of the cart item") @PathVariable UUID itemId,
                                      @RequestBody ChangeItemQuantityRequest request) {
        return cartMapper.toDto(shopService.changeItemQuantity(id, itemId, request.getQuantity()));
    }

    @Operation(summary = "Remove item from cart", description = "Removes the item with its add-ons from the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found"),
            @ApiResponse(responseCode = "409", description = "Cart already checked out"),
            @ApiResponse(responseCode = "503", description = "Cart service is unavailable")
    })
    @DeleteMapping("/{id}/items/{itemId}")
    public CartDto removeItem(@Parameter(description = "Id of the cart") @PathVariable UUID id,
                              @Parameter(description = "Id of the cart item") @PathVariable UUID itemId) {
        return cartMapper.toDto(shopService.removeItem(id, itemId));
    }
}
