package org.babyfish.jimmer.model;

import org.babyfish.jimmer.ImmutableObjects;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SamePasswordValidator implements ConstraintValidator<SamePassword, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest value, ConstraintValidatorContext context) {
        if (ImmutableObjects.isLoaded(value, "password") && ImmutableObjects.isLoaded(value, "passwordAgain")) {
            return value.password().equals(value.passwordAgain());
        }
        return true;
    }
}
