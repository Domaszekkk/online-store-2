package com.estore.invoice.document;

import com.estore.invoice.VatRate;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "document_vat_summaries")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VatSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_rate", length = 20)
    private VatRate vatRate;

    @Column(name = "net_total", precision = 19, scale = 2)
    private BigDecimal netTotal;

    @Column(name = "vat_total", precision = 19, scale = 2)
    private BigDecimal vatTotal;

    @Column(name = "gross_total", precision = 19, scale = 2)
    private BigDecimal grossTotal;
}
