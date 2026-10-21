package com.estore.order;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CompleteCompanyDataValidator implements ConstraintValidator<CompleteCompanyData, BillingAddressRequest> {

    @Override
    public boolean isValid(BillingAddressRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        boolean hasCompanyName = isPresent(request.getCompanyName());
        boolean hasTaxIdentificationNumber = isPresent(request.getTaxIdentificationNumber());
        if (hasCompanyName == hasTaxIdentificationNumber) {
            return true;
        }
        String missingField = hasCompanyName ? "taxIdentificationNumber" : "companyName";
        String providedField = hasCompanyName ? "companyName" : "taxIdentificationNumber";
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("must be provided together with " + providedField)
                .addPropertyNode(missingField)
                .addConstraintViolation();
        return false;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
