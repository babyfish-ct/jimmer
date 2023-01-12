package org.babyfish.jimmer.pojo;

/**
 * How to handle scalar fields that are not decorated by `@Static` explicitly.
 *
 * For id field, if the entity type has key, it will be mapped as
 * optional static field implicitly
 */
public enum AutoScalarStrategy {

    /**
     * Mapping all scalar fields of both current type and super types,
     * except scalar fields decorated by `@Static(enabled = false)`
     */
    ALL,

    /**
     * Mapping all scalar fields of current types,
     * except scalar fields decorated by `@Static(enabled = false)` and
     * scalar fields declared in super types
     */
    DECLARED,

    /**
     * Scalar field will not be mapped automatically,
     * unless it is decorated by `@Static(enabled = true)` explicitly
     */
    NONE
}
