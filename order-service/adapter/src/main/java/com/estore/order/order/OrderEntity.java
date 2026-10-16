package com.estore.order.order;

import com.estore.order.DeliveryMethod;
import com.estore.order.OrderStatus;
import com.estore.order.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "order_number", length = 20, unique = true)
    private String orderNumber;

    @Column(name = "cart_id")
    private UUID cartId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private OrderStatus status;

    @Embedded
    private CustomerEmbeddable customer;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "shipping_street")),
            @AttributeOverride(name = "city", column = @Column(name = "shipping_city", length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "shipping_country", length = 100))
    })
    private AddressEmbeddable shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city", length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal_code", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "billing_country", length = 100))
    })
    private AddressEmbeddable billingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", length = 50)
    private DeliveryMethod deliveryMethod;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    @OrderColumn(name = "line_number")
    @Builder.Default
    private List<OrderLineEntity> lines = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "items_total_amount", precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "items_total_currency", length = 3))
    })
    private MoneyEmbeddable itemsTotal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "delivery_cost_amount", precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "delivery_cost_currency", length = 3))
    })
    private MoneyEmbeddable deliveryCost;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "grand_total_amount", precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "grand_total_currency", length = 3))
    })
    private MoneyEmbeddable grandTotal;

    @Column(name = "created_at")
    private Instant createdAt;
}
