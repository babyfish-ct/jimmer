package org.babyfish.jimmer.sql.ast.mutation;

/**
 * Specifies how mutation commands match inheritance entity types.
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
