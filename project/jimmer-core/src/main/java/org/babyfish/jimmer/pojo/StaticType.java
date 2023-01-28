package org.babyfish.jimmer.pojo;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(StaticTypes.class)
public @interface StaticType {

    /**
     * A short name for the static type,
     * this name will only be used in mapping system internal,
     * generated source code cannot be affected
     */
    String alias();

    /**
     * If `topLevelName` is not empty,
     * a top level static class will be generated;
     *
     * Otherwise, top level static class will not be
     * generated, and it can only be referenced by
     * other static classes
     */
    String topLevelName() default "";

    /**
     * How to handle scalar fields that are not decorated by @{@link Static} explicitly.
     *
     * For id field, if the entity type has key, it will be mapped as
     * optional static field implicitly
     */
    AutoScalarStrategy autoScalarStrategy() default AutoScalarStrategy.ALL;

    boolean allOptional() default false;
}
