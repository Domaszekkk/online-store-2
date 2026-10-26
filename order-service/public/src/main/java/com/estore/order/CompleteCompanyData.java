package com.estore.order;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CompleteCompanyDataValidator.class)
public @interface CompleteCompanyData {
    String message() default "companyName and taxIdentificationNumber must be provided together";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
