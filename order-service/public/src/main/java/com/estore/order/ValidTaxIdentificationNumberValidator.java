package com.estore.order;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidTaxIdentificationNumberValidator implements ConstraintValidator<ValidTaxIdentificationNumber, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || BillingAddress.isValidTaxIdentificationNumber(value);
    }
}
