package com.estore.gateway.product;

import com.estore.gateway.PageDto;
import com.estore.gateway.ProductDetailsDto;
import com.estore.gateway.ProductSummaryDto;
import com.estore.gateway.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/shop/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product list and product configuration for the shop frontend")
public class ProductController {
    private final ShopService shopService;
    private final ProductMapper productMapper;

    @Operation(summary = "Get products",
            description = "Screen 1: returns one page of the product list sorted by name, optionally filtered by product type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of products returned (possibly empty)"),
            @ApiResponse(responseCode = "400", description = "Unknown product type, negative page or page size outside 1-100"),
            @ApiResponse(responseCode = "503", description = "Product service is unavailable")
    })
    @GetMapping
    public PageDto<ProductSummaryDto> getProducts(@Parameter(description = "Product type to filter by (optional)")
                                                  @RequestParam(required = false) String type,
                                                  @Parameter(description = "Page number, starting from 0")
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @Parameter(description = "Page size, from 1 to 100")
                                                  @RequestParam(defaultValue = "20") int size) {
        return productMapper.toSummaryPageDto(shopService.getProducts(type, page, size));
    }

    @Operation(summary = "Get product", description = "Returns options, variants and add-ons needed to configure the product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "503", description = "Product service is unavailable")
    })
    @GetMapping("/{id}")
    public ProductDetailsDto getProduct(@Parameter(description = "Id of the product") @PathVariable UUID id) {
        return productMapper.toDto(shopService.getProduct(id));
    }
}
