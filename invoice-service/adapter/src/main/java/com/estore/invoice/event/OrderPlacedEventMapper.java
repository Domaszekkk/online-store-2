package com.estore.invoice.event;

import com.estore.invoice.IssueDocumentCommand;
import com.estore.invoice.Money;
import com.estore.invoice.OrderBillingAddress;
import com.estore.invoice.OrderCustomer;
import com.estore.invoice.OrderedAddOn;
import com.estore.invoice.OrderedLine;
import com.estore.invoice.VatRate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderPlacedEventMapper {

    public IssueDocumentCommand toCommand(OrderPlacedEvent event) {
        return IssueDocumentCommand.builder()
                .orderId(event.orderId())
                .orderNumber(event.orderNumber())
                .placedAt(event.placedAt())
                .customer(OrderCustomer.builder()
                        .firstName(event.customer().firstName())
                        .lastName(event.customer().lastName())
                        .email(event.customer().email())
                        .build())
                .billingAddress(billingAddress(event.billingAddress()))
                .paymentMethod(event.paymentMethod())
                .deliveryMethod(event.deliveryMethod())
                .deliveryCost(money(event.deliveryCost()))
                .deliveryVatRate(VatRate.valueOf(event.deliveryVatRate()))
                .lines(event.lines().stream().map(this::line).toList())
                .build();
    }

    private OrderBillingAddress billingAddress(OrderPlacedEvent.BillingAddress billingAddress) {
        if (billingAddress == null) {
            return null;
        }
        return OrderBillingAddress.builder()
                .companyName(billingAddress.companyName())
                .taxIdentificationNumber(billingAddress.taxIdentificationNumber())
                .street(billingAddress.street())
                .city(billingAddress.city())
                .postalCode(billingAddress.postalCode())
                .country(billingAddress.country())
                .build();
    }

    private OrderedLine line(OrderPlacedEvent.Line line) {
        List<OrderPlacedEvent.AddOn> addOns = line.addOns() == null ? List.of() : line.addOns();
        return OrderedLine.builder()
                .productName(line.productName())
                .stockKeepingUnit(line.stockKeepingUnit())
                .unitPrice(money(line.unitPrice()))
                .vatRate(VatRate.valueOf(line.vatRate()))
                .quantity(line.quantity())
                .addOns(addOns.stream().map(this::addOn).toList())
                .build();
    }

    private OrderedAddOn addOn(OrderPlacedEvent.AddOn addOn) {
        return OrderedAddOn.builder()
                .name(addOn.name())
                .price(money(addOn.price()))
                .vatRate(VatRate.valueOf(addOn.vatRate()))
                .quantity(addOn.quantity())
                .build();
    }

    private Money money(OrderPlacedEvent.Money money) {
        return Money.of(money.amount(), money.currency());
    }
}
