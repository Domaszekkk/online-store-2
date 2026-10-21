package com.estore.invoice.document;

import com.estore.invoice.VatRate;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "document_lines")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_number")
    private int position;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "stock_keeping_unit", length = 255)
    private String stockKeepingUnit;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "unit_gross_price", precision = 19, scale = 2)
    private BigDecimal unitGrossPrice;

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
