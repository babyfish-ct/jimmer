package org.babyfish.jimmer.model;

import javax.validation.Constraint;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Constraint(validatedBy = SamePasswordValidator.class)
public @interface SamePassword {
}
