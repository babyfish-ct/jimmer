package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface GeneratedValue {

    GenerationType strategy() default GenerationType.AUTO;

    Class<? extends UserIdGenerator<?>> generatorType() default UserIdGenerator.None.class;

    String sequenceName() default "";
}
