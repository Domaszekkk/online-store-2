package com.estore.cart.cart;

import com.estore.cart.AddItemRequest;
import com.estore.cart.CartDto;
import com.estore.cart.CartService;
import com.estore.cart.ChangeItemQuantityRequest;
import com.estore.cart.CheckoutRequest;
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
@RequestMapping("/carts")
@RequiredArgsConstructor
@Tag(name = "Cart Management", description = "Endpoints for managing shopping carts")
public class CartController {
    private final CartService cartService;
    private final CartMapper cartMapper;

    @Operation(summary = "Create a new cart",
            description = "Creates a new empty shopping cart; an active cart expires after 30 minutes without changes or order preview "
                    + "and then every change returns 409")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cart created successfully")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartDto createCart() {
        return cartMapper.toDto(cartService.createCart());
    }

    @Operation(summary = "Get cart by id", description = "Returns a single cart identified by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart found"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/{id}")
    public CartDto getCartById(@Parameter(description = "Id of the cart to retrieve") @PathVariable UUID id) {
        return cartMapper.toDto(cartService.getCartById(id));
    }

    @Operation(summary = "Add item to cart",
            description = "Adds a product variant (with optional add-ons) to the cart; price and name are snapshotted from the product service")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added (or quantity increased if the same configuration is already in the cart)"),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g. missing ids, quantity lower than 1)"),
            @ApiResponse(responseCode = "404", description = "Cart, product or variant not found"),
            @ApiResponse(responseCode = "409", description = "Variant is not available for purchase or cart is no longer active"),
            @ApiResponse(responseCode = "503", description = "Product service is unavailable")
    })
    @PostMapping("/{id}/items")
    public CartDto addItem(@Parameter(description = "Id of the cart") @PathVariable UUID id,
                           @Valid @RequestBody AddItemRequest request) {
        return cartMapper.toDto(cartService.addItem(id, cartMapper.toCommand(request)));
    }

    @Operation(summary = "Change item quantity", description = "Sets a new quantity for an item already in the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quantity changed"),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g. missing ids, quantity lower than 1)"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found"),
            @ApiResponse(responseCode = "409", description = "Cart is no longer active")
    })
    @PatchMapping("/{id}/items/{itemId}")
    public CartDto changeItemQuantity(@Parameter(description = "Id of the cart") @PathVariable UUID id,
                                      @Parameter(description = "Id of the cart item") @PathVariable UUID itemId,
                                      @Valid @RequestBody ChangeItemQuantityRequest request) {
        return cartMapper.toDto(cartService.changeItemQuantity(id, itemId, cartMapper.toCommand(request)));
    }

    @Operation(summary = "Remove item from cart", description = "Removes an item from the cart and returns the updated cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found"),
            @ApiResponse(responseCode = "409", description = "Cart is no longer active")
    })
    @DeleteMapping("/{id}/items/{itemId}")
    public CartDto removeItem(@Parameter(description = "Id of the cart") @PathVariable UUID id,
                              @Parameter(description = "Id of the cart item") @PathVariable UUID itemId) {
        return cartMapper.toDto(cartService.removeItem(id, itemId));
    }

    @Operation(summary = "Refresh cart with the catalog",
            description = "Brings prices and VAT rates of the items up to date with the product service and marks items whose product, "
                    + "variant or add-on is no longer available; the version grows when anything changes. Used by order-service for the order summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart refreshed (a checked out cart is returned unchanged)"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "503", description = "Product service is unavailable")
    })
    @PostMapping("/{id}/refresh")
    public CartDto refresh(@Parameter(description = "Id of the cart") @PathVariable UUID id) {
        return cartMapper.toDto(cartService.refresh(id));
    }

    @Operation(summary = "Check out cart",
            description = "Closes the cart for ordering (status CHECKED_OUT) and returns its final content; used by order-service when an order is placed. "
                    + "The cart must still have the version shown in the order summary and is checked against the catalog first. "
                    + "Idempotent: calling it again for a checked out cart returns the same content, so order-service can safely retry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart checked out (now or earlier)"),
            @ApiResponse(responseCode = "400", description = "Missing expected version"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "409", description = "Cart is empty, has changed since the summary or contains unavailable products"),
            @ApiResponse(responseCode = "503", description = "Product service is unavailable")
    })
    @PostMapping("/{id}/checkout")
    public CartDto checkout(@Parameter(description = "Id of the cart") @PathVariable UUID id,
                            @Valid @RequestBody CheckoutRequest request) {
        return cartMapper.toDto(cartService.checkout(id, request.getExpectedVersion()));
    }
}
