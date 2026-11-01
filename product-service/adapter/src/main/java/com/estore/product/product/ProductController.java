package com.estore.product.product;

import com.estore.product.AddProductRequest;
import com.estore.product.PageDto;
import com.estore.product.PageQuery;
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

    @Operation(summary = "Get products",
            description = "Returns one page of products on sale sorted by name, optionally filtered by type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of products returned (possibly empty)"),
            @ApiResponse(responseCode = "400", description = "Unknown type, negative page or page size outside 1-100")
    })
    @GetMapping
    public PageDto<ProductDto> getProducts(
            @Parameter(description = "Product type to filter by (COMPUTER, SMARTPHONE, ELECTRONICS)")
            @RequestParam(required = false) ProductType type,
            @Parameter(description = "Page number, starting from 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, from 1 to 100") @RequestParam(defaultValue = "20") int size) {
        return productMapper.toPageDto(productService.getProducts(type, new PageQuery(page, size)));
    }

    @Operation(summary = "Get archived products",
            description = "Returns one page of products withdrawn from sale that are still kept for the retention period, newest first")
    @GetMapping("/archived")
    public PageDto<ProductDto> getArchivedProducts(
            @Parameter(description = "Page number, starting from 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, from 1 to 100") @RequestParam(defaultValue = "20") int size) {
        return productMapper.toPageDto(productService.getArchivedProducts(new PageQuery(page, size)));
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

    @Operation(summary = "Create a new product",
            description = "Creates a new product with its options, variants and add-ons; variant ids are assigned by the service. "
                    + "ELECTRONICS has exactly one variant and no options or add-ons; in other types every variant "
                    + "has one allowed value for each option. An add-on is given only by the id of an ELECTRONICS product; "
                    + "its name, price, VAT rate and availability are always taken from that product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or add-on referencing a non-existent product"),
            @ApiResponse(responseCode = "409", description = "Stock keeping unit already used by another product on sale")
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
            @ApiResponse(responseCode = "409", description = "Stock keeping unit already used by another product on sale"),
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
