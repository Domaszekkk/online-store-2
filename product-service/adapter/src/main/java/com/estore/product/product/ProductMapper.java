package com.estore.product.product;

import com.estore.product.AddProductCommand;
import com.estore.product.AddProductRequest;
import com.estore.product.Product;
import com.estore.product.ProductDto;
import com.estore.product.ProductOption;
import com.estore.product.ProductOptionDto;
import com.estore.product.UpdateProductCommand;
import com.estore.product.UpdateProductRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProductMapper {

    ProductDto toDto(Product product);

    List<ProductDto> toDtos(List<Product> products);

    ProductEntity toEntity(Product product);

    Product toModel(ProductEntity entity);

    List<ProductOptionDto> toOptionDtos(List<ProductOption> options);

    AddProductCommand toCommand(AddProductRequest request);

    UpdateProductCommand toCommand(UpdateProductRequest request);
}
