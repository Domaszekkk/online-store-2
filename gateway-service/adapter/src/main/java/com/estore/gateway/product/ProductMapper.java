package com.estore.gateway.product;

import com.estore.gateway.ProductDetails;
import com.estore.gateway.ProductDetailsDto;
import com.estore.gateway.ProductSummary;
import com.estore.gateway.ProductSummaryDto;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProductMapper {

    List<ProductSummary> toSummaries(List<ProductResponse> responses);

    ProductSummary toSummary(ProductResponse response);

    ProductDetails toDetails(ProductResponse response);

    List<ProductSummaryDto> toSummaryDtos(List<ProductSummary> products);

    ProductDetailsDto toDto(ProductDetails product);
}
