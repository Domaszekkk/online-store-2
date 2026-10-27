package com.estore.product;

import java.util.Collection;

public class StockKeepingUnitTakenException extends ProductException {
    public StockKeepingUnitTakenException(Collection<String> stockKeepingUnits) {
        super("Stock keeping units " + stockKeepingUnits + " are already used by another product on sale", 409);
    }
}
