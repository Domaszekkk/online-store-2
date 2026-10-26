package com.estore.product.product;

import com.estore.product.AddProductRequest;
import com.estore.product.ProductDto;
import com.estore.product.ProductOptionDto;
import com.estore.product.ProductService;
import com.estore.product.ProductType;
import com.estore.product.UpdateProductRequest;
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
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Endpoints for managing products")
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;

    @Operation(summary = "Get products", description = "Returns all products, optionally filtered by type")
    @GetMapping
    public List<ProductDto> getProducts(
            @Parameter(description = "Product type to filter by (COMPUTER, SMARTPHONE, ELECTRONICS)")
            @RequestParam(required = false) ProductType type) {
        return productMapper.toDtos(productService.getProducts(type));
    }

    @Operation(summary = "Get archived products",
            description = "Returns products withdrawn from sale that are still kept for the retention period")
    @GetMapping("/archived")
    public List<ProductDto> getArchivedProducts() {
        return productMapper.toDtos(productService.getArchivedProducts());
    }

    @Operation(summary = "Get archived product by id",
            description = "Returns a single product withdrawn from sale, together with the date it was archived")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archived product found"),
            @ApiResponse(responseCode = "404", description = "Archived product not found")
    })
    @GetMapping("/archived/{id}")
    public ProductDto getArchivedProductById(
            @Parameter(description = "Id of the archived product to retrieve") @PathVariable UUID id) {
        return productMapper.toDto(productService.getArchivedProductById(id));
    }

    @Operation(summary = "Get product by id", description = "Returns a single product identified by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ProductDto getProductById(@Parameter(description = "Id of the product to retrieve") @PathVariable UUID id) {
        return productMapper.toDto(productService.getProductById(id));
    }

    @Operation(summary = "Get product configuration",
            description = "Returns the configurable options (e.g. processor, RAM) available for the product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration returned"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}/configuration")
    public List<ProductOptionDto> getProductConfiguration(
            @Parameter(description = "Id of the product") @PathVariable UUID id) {
        return productMapper.toOptionDtos(productService.getProductConfiguration(id));
    }

    @Operation(summary = "Create a new product", description = "Creates a new product with its options, variants and add-ons")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or add-on referencing a non-existent product")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto addProduct(@Valid @RequestBody AddProductRequest request) {
        return productMapper.toDto(productService.addProduct(productMapper.toCommand(request)));
    }

    @Operation(summary = "Update product", description = "Updates the details of an existing product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or add-on referencing a non-existent product"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    public ProductDto updateProduct(
            @Parameter(description = "Id of the product to update") @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return productMapper.toDto(productService.updateProduct(id, productMapper.toCommand(request)));
    }

    @Operation(summary = "Withdraw product from sale",
            description = "Archives the product and removes it from the add-ons of other products. "
                    + "The archived product is kept for the retention period and then deleted permanently")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product archived successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveProduct(@Parameter(description = "Id of the product to withdraw from sale") @PathVariable UUID id) {
        productService.archiveProduct(id);
    }
}
