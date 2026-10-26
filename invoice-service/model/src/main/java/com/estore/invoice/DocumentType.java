package com.estore.invoice;

import java.time.YearMonth;

public enum DocumentType {
    INVOICE("FV"),
    PURCHASE_CONFIRMATION("PZ");

    private final String numberPrefix;

    DocumentType(String numberPrefix) {
        this.numberPrefix = numberPrefix;
    }

    public String formatNumber(YearMonth period, long sequenceNumber) {
        return "%s/%d/%02d/%06d".formatted(numberPrefix, period.getYear(), period.getMonthValue(), sequenceNumber);
    }
}
