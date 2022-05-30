package org.babyfish.jimmer.sql;

public enum CascadeAction {

    NONE,
    SET_NULL,
    DELETE,

    /**
     * If many-to-one property is nullable, means SET_NULL,
     * otherwise, means CASCADE
     */
    AUTO
}
