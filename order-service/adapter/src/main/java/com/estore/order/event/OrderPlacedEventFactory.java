package com.estore.order.event;

import com.estore.order.BillingAddress;
import com.estore.order.Money;
import com.estore.order.Order;
import com.estore.order.OrderLine;
import com.estore.order.OrderLineAddOn;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderPlacedEventFactory {

    public OrderPlacedEvent create(Order order) {
        return new OrderPlacedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt(),
                new CustomerPayload(
                        order.getCustomer().getFirstName(),
                        order.getCustomer().getLastName(),
                        order.getCustomer().getEmail(),
                        order.getCustomer().getPhone()),
                billingAddress(order.getBillingAddress()),
                order.getPaymentMethod().name(),
                order.getDeliveryMethod().name(),
                money(order.getDeliveryCost()),
                order.getDeliveryMethod().vatRate().name(),
                order.getLines().stream().map(this::line).toList(),
                money(order.getItemsTotal()),
                money(order.getGrandTotal()));
    }

    private BillingAddressPayload billingAddress(BillingAddress billingAddress) {
        if (billingAddress == null) {
            return null;
        }
        return new BillingAddressPayload(
                billingAddress.getCompanyName(),
                billingAddress.getTaxIdentificationNumber(),
                billingAddress.getStreet(),
                billingAddress.getCity(),
                billingAddress.getPostalCode(),
                billingAddress.getCountry());
    }

    private OrderLinePayload line(OrderLine line) {
        return new OrderLinePayload(
                line.getProductId(),
                line.getVariantId(),
                line.getProductName(),
                line.getStockKeepingUnit(),
                money(line.getUnitPrice()),
                line.getVatRate().name(),
                line.getQuantity(),
                line.getAddOns().stream().map(this::addOn).toList(),
                money(line.getLineTotal()));
    }

    private AddOnPayload addOn(OrderLineAddOn addOn) {
        return new AddOnPayload(
                addOn.getProductId(),
                addOn.getName(),
                money(addOn.getPrice()),
                addOn.getVatRate().name(),
                addOn.getQuantity());
    }

    private MoneyPayload money(Money money) {
        return new MoneyPayload(money.amount(), money.currency());
    }
}
