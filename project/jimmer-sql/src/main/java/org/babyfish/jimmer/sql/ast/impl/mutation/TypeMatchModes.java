package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.TypeMatchMode;
import org.babyfish.jimmer.sql.exception.ExecutionException;

final class TypeMatchModes {

    private TypeMatchModes() {}

    static TypeMatchMode resolve(ImmutableType type, TypeMatchMode mode) {
        if (mode == null || mode == TypeMatchMode.AUTO) {
            return type.isInstantiable() ? TypeMatchMode.EXACT : TypeMatchMode.POLYMORPHIC;
        }
        return mode;
    }

    static void validateExact(ImmutableType type, TypeMatchMode mode, String operation) {
        if (resolve(type, mode) == TypeMatchMode.EXACT && !type.isInstantiable()) {
            throw new ExecutionException(
                    "Cannot " +
                            operation +
                            " inheritance entity type \"" +
                            type +
                            "\" exactly because it is abstract"
            );
        }
    }

    static boolean isPolymorphic(ImmutableType type, TypeMatchMode mode) {
        return resolve(type, mode) == TypeMatchMode.POLYMORPHIC;
    }
}
