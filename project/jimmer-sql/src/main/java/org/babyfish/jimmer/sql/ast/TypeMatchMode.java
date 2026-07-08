package org.babyfish.jimmer.sql.ast;

/**
 * Specifies how inheritance entity types are matched by mutation commands and root queries.
 */
public enum TypeMatchMode {

    /**
     * Match the exact type for instantiable entity types, or all instantiable
     * types for abstract inheritance types.
     */
    AUTO,

    /**
     * Match only the exact entity type. This is invalid for abstract
     * inheritance entity types.
     */
    EXACT,

    /**
     * Match the entity type and all instantiable derived types.
     */
    POLYMORPHIC
}
