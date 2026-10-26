package com.estore.order;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLine {
    private UUID id;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String stockKeepingUnit;
    private Money unitPrice;
    private VatRate vatRate;
    private int quantity;

    @Builder.Default
    private List<OrderLineAddOn> addOns = new ArrayList<>();

    private Money lineTotal;

    public static OrderLine fromCartItem(CartItemDetails item) {
        List<OrderLineAddOn> addOns = (item.getAddOns() == null ? List.<CartItemAddOnDetails>of() : item.getAddOns())
                .stream()
                .map(addOn -> OrderLineAddOn.builder()
                        .productId(addOn.getProductId())
                        .name(addOn.getName())
                        .price(addOn.getPrice())
                        .vatRate(addOn.getVatRate())
                        .quantity(addOn.getQuantity())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        OrderLine line = OrderLine.builder()
                .id(UUID.randomUUID())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .productName(item.getProductName())
                .stockKeepingUnit(item.getStockKeepingUnit())
                .unitPrice(item.getUnitPrice())
                .vatRate(item.getVatRate())
                .quantity(item.getQuantity())
                .addOns(addOns)
                .build();
        line.lineTotal = line.computeLineTotal();
        return line;
    }

    private Money computeLineTotal() {
        Money addOnsTotal = addOns.stream()
                .map(OrderLineAddOn::total)
                .reduce(Money.zero(unitPrice.currency()), Money::add);
        return unitPrice.multiply(quantity).add(addOnsTotal);
    }
}
