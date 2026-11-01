package com.estore.gateway.product;

import com.estore.gateway.PageDto;
import com.estore.gateway.ProductDetails;
import com.estore.gateway.ProductDetailsDto;
import com.estore.gateway.ProductSummary;
import com.estore.gateway.ProductSummaryDto;
import com.estore.gateway.ResultPage;
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

    default ResultPage<ProductSummary> toSummaryPage(ProductPageResponse response) {
        return new ResultPage<>(toSummaries(response.items()), response.page(), response.size(),
                response.totalItems(), response.totalPages());
    }

    default PageDto<ProductSummaryDto> toSummaryPageDto(ResultPage<ProductSummary> page) {
        return PageDto.<ProductSummaryDto>builder()
                .items(toSummaryDtos(page.items()))
                .page(page.page())
                .size(page.size())
                .totalItems(page.totalItems())
                .totalPages(page.totalPages())
                .build();
    }
}
