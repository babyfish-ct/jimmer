package org.babyfish.jimmer.pojo;

import java.lang.annotation.*;

/**
 * How to handle scalar fields that are not decorated by @{@link Static} explicitly.
 *
 * For id field, if the entity type has key, it will be mapped as
 * optional static field implicitly
 *
 * This is designed for @{@link org.babyfish.jimmer.sql.MappedSuperclass}.
 * For @{@link org.babyfish.jimmer.sql.Entity}, you can also use
 * {@link StaticType#autoScalarStrategy()}
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(AutoScalarRules.class)
public @interface AutoScalarRule {

    AutoScalarStrategy value();

    String alias() default "";
}
