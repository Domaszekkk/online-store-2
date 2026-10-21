package com.estore.invoice;

import java.time.YearMonth;

public interface DocumentNumberPort {
    long nextSequenceNumber(DocumentType type, YearMonth period);
}
