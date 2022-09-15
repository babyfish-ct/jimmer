package org.babyfish.jimmer.model;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IdCardValidator implements ConstraintValidator<IdCard, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return false;
    }
}
