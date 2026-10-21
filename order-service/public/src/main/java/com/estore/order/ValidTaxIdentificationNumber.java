package com.estore.order;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTaxIdentificationNumberValidator.class)
public @interface ValidTaxIdentificationNumber {
    String message() default "must be 10 digits with a correct checksum";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
