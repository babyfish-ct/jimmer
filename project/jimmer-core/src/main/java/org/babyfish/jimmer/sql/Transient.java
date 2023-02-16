package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;
import org.babyfish.jimmer.Formula;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface Transient {

    /**
     * If this argument is not specified, the decorated property is a transient property,
     * otherwise, the decorated property is a complex calculation property.
     *
     * For simple calculation property, please view {@link Formula}
     *
     * When this argument is specified, it must be
     * `org.babyfish.jimmer.sql.TransientResolver` of jimmer-sql or
     * `org.babyfish.jimmer.sql.kt.KTransientResolver` of jimmer-sql-kotlin.
     *
     * @return A class implements
     * `org.babyfish.jimmer.sql.TransientResolver` of jimmer-sql or
     * `org.babyfish.jimmer.sql.kt.KTransientResolver` of jimmer-sql-kotlin.
     */
    Class<?> value() default void.class;
}
