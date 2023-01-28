package org.babyfish.jimmer.pojo;

public enum AutoScalarStrategy {

    /**
     * Mapping all scalar fields of both current type and super types,
     * except scalar fields decorated by `@Static(enabled = false)`
     * <p>
     * This is the default behavior when `@Entity` or `@MappedSuperClass`
     * is not decorated by `@StaticAutoScalar`
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
