package org.babyfish.jimmer.sql;

public enum DeleteAction {

    NONE,
    SET_NULL,
    CASCADE,

    /**
     * If many-to-one property is nullable, means SET_NULL,
     * otherwise, means CASCADE
     */
    AUTO
}
