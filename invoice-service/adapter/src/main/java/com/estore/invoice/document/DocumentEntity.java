package com.estore.invoice.document;

import com.estore.invoice.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private DocumentType type;

    @Column(name = "number", length = 30, unique = true)
    private String number;

    @Column(name = "order_id", unique = true)
    private UUID orderId;

    @Column(name = "order_number", length = 20)
    private String orderNumber;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "sale_date")
    private Instant saleDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "delivery_method", length = 50)
    private String deliveryMethod;

    @Embedded
    private SellerEmbeddable seller;

    @Embedded
    private BuyerEmbeddable buyer;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "document_id")
    @OrderBy("position")
    @Builder.Default
    private List<DocumentLineEntity> lines = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "document_id")
    @OrderColumn(name = "summary_order")
    @Builder.Default
    private List<VatSummaryEntity> vatSummaries = new ArrayList<>();

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "net_total", precision = 19, scale = 2)
    private BigDecimal netTotal;

    @Column(name = "vat_total", precision = 19, scale = 2)
    private BigDecimal vatTotal;

    @Column(name = "gross_total", precision = 19, scale = 2)
    private BigDecimal grossTotal;
}
