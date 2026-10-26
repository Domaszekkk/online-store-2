package com.estore.invoice;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Document {
    public static final String CURRENCY = "PLN";

    private UUID id;
    private DocumentType type;
    private String number;
    private UUID orderId;
    private String orderNumber;
    private Instant issuedAt;
    private Instant saleDate;
    private String paymentMethod;
    private String deliveryMethod;
    private Seller seller;
    private Buyer buyer;

    @Builder.Default
    private List<DocumentLine> lines = new ArrayList<>();

    @Builder.Default
    private List<VatSummary> vatSummaries = new ArrayList<>();

    private Money netTotal;
    private Money vatTotal;
    private Money grossTotal;

    public static DocumentType typeFor(IssueDocumentCommand command) {
        return command.isInvoiceRequested() ? DocumentType.INVOICE : DocumentType.PURCHASE_CONFIRMATION;
    }

    public static Document issue(IssueDocumentCommand command, Seller seller, String number, Instant issuedAt) {
        List<DocumentLine> lines = linesOf(command);
        List<VatSummary> vatSummaries = summarize(lines);
        return Document.builder()
                .id(UUID.randomUUID())
                .type(typeFor(command))
                .number(number)
                .orderId(command.getOrderId())
                .orderNumber(command.getOrderNumber())
                .issuedAt(issuedAt)
                .saleDate(command.getPlacedAt())
                .paymentMethod(command.getPaymentMethod())
                .deliveryMethod(command.getDeliveryMethod())
                .seller(seller)
                .buyer(command.isInvoiceRequested() ? buyerOf(command) : null)
                .lines(lines)
                .vatSummaries(vatSummaries)
                .netTotal(sum(vatSummaries.stream().map(VatSummary::getNetTotal).toList()))
                .vatTotal(sum(vatSummaries.stream().map(VatSummary::getVatTotal).toList()))
                .grossTotal(sum(vatSummaries.stream().map(VatSummary::getGrossTotal).toList()))
                .build();
    }

    private static List<DocumentLine> linesOf(IssueDocumentCommand command) {
        List<DocumentLine> lines = new ArrayList<>();
        for (OrderedLine orderedLine : command.getLines()) {
            lines.add(DocumentLine.of(lines.size() + 1, orderedLine.getProductName(), orderedLine.getStockKeepingUnit(),
                    orderedLine.getQuantity(), orderedLine.getUnitPrice(), orderedLine.getVatRate()));
            for (OrderedAddOn addOn : orderedLine.getAddOns()) {
                lines.add(DocumentLine.of(lines.size() + 1, addOn.getName(), null,
                        addOn.getQuantity(), addOn.getPrice(), addOn.getVatRate()));
            }
        }
        if (command.getDeliveryCost() != null && command.getDeliveryCost().isPositive()) {
            lines.add(DocumentLine.of(lines.size() + 1, deliveryLineName(command.getDeliveryMethod()), null,
                    1, command.getDeliveryCost(), command.getDeliveryVatRate()));
        }
        return lines;
    }

    private static List<VatSummary> summarize(List<DocumentLine> lines) {
        Map<VatRate, Money> grossPerRate = new EnumMap<>(VatRate.class);
        for (DocumentLine line : lines) {
            grossPerRate.merge(line.getVatRate(), line.getGrossTotal(), Money::add);
        }
        return grossPerRate.entrySet().stream()
                .map(entry -> VatSummary.fromGross(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static Buyer buyerOf(IssueDocumentCommand command) {
        OrderBillingAddress billingAddress = command.getBillingAddress();
        OrderCustomer customer = command.getCustomer();
        return Buyer.builder()
                .fullName(customer.getFirstName() + " " + customer.getLastName())
                .companyName(billingAddress.getCompanyName())
                .taxIdentificationNumber(billingAddress.getTaxIdentificationNumber())
                .street(billingAddress.getStreet())
                .city(billingAddress.getCity())
                .postalCode(billingAddress.getPostalCode())
                .country(billingAddress.getCountry())
                .email(customer.getEmail())
                .build();
    }

    private static String deliveryLineName(String deliveryMethod) {
        return switch (deliveryMethod) {
            case "COURIER" -> "Dostawa kurierem";
            case "PARCEL_LOCKER" -> "Dostawa do paczkomatu";
            default -> "Dostawa";
        };
    }

    private static Money sum(List<Money> amounts) {
        return amounts.stream().reduce(Money.zero(CURRENCY), Money::add);
    }
}
