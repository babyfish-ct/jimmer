package org.babyfish.jimmer.sql.transaction;

public enum Propagation {
    REQUIRED,
    REQUIRES_NEW,
    SUPPORTS,
    NOT_SUPPORTED,
    NEVER,
    MANDATORY
}
