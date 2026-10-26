package com.estore.invoice;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentDto {
    private UUID id;
    private String type;
    private String number;
    private UUID orderId;
    private String orderNumber;
    private Instant issuedAt;
    private Instant saleDate;
    private String paymentMethod;
    private String deliveryMethod;
    private SellerDto seller;
    private BuyerDto buyer;
    private List<DocumentLineDto> lines;
    private List<VatSummaryDto> vatSummaries;
    private MoneyDto netTotal;
    private MoneyDto vatTotal;
    private MoneyDto grossTotal;
}
