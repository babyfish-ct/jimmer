package org.babyfish.jimmer.sql.ast.impl.mutation.save;

enum DisconnectingType {
    CHECKING,
    NONE,
    SET_NULL,
    LOGICAL_DELETE,
    PHYSICAL_DELETE;
    boolean isDelete() {
        return this == LOGICAL_DELETE || this == PHYSICAL_DELETE;
    }
}
