package com.estore.invoice;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IssueDocumentCommand {
    private UUID orderId;
    private String orderNumber;
    private Instant placedAt;
    private OrderCustomer customer;
    private OrderBillingAddress billingAddress;
    private String paymentMethod;
    private String deliveryMethod;
    private Money deliveryCost;
    private VatRate deliveryVatRate;

    @Builder.Default
    private List<OrderedLine> lines = new ArrayList<>();

    public boolean isInvoiceRequested() {
        return billingAddress != null;
    }
}
