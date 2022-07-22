package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;
import org.babyfish.jimmer.sql.meta.IdGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface GeneratedValue {
    GenerationType strategy();
    Class<? extends IdGenerator> generatorType() default IdGenerator.None.class;
    String sequenceName() default "";
}
