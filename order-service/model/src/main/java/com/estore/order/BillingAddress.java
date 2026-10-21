package com.estore.order;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillingAddress {
    private static final int[] TAX_IDENTIFICATION_NUMBER_WEIGHTS = {6, 5, 7, 2, 3, 4, 5, 6, 7};

    private String companyName;
    private String taxIdentificationNumber;
    private String street;
    private String city;
    private String postalCode;
    private String country;

    public boolean isCompany() {
        return isPresent(taxIdentificationNumber);
    }

    public void requireValid() {
        boolean hasCompanyName = isPresent(companyName);
        boolean hasTaxIdentificationNumber = isPresent(taxIdentificationNumber);
        if (hasCompanyName != hasTaxIdentificationNumber) {
            throw new InvalidBillingAddressException("companyName and taxIdentificationNumber must be provided together");
        }
        if (hasTaxIdentificationNumber && !isValidTaxIdentificationNumber(taxIdentificationNumber)) {
            throw new InvalidBillingAddressException(
                    "taxIdentificationNumber " + taxIdentificationNumber + " must be 10 digits with a correct checksum");
        }
    }

    public static boolean isValidTaxIdentificationNumber(String value) {
        if (value == null || !value.matches("\\d{10}")) {
            return false;
        }
        int sum = 0;
        for (int position = 0; position < TAX_IDENTIFICATION_NUMBER_WEIGHTS.length; position++) {
            sum += Character.getNumericValue(value.charAt(position)) * TAX_IDENTIFICATION_NUMBER_WEIGHTS[position];
        }
        int checkDigit = sum % 11;
        return checkDigit != 10 && checkDigit == Character.getNumericValue(value.charAt(9));
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
