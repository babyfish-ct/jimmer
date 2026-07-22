package org.babyfish.jimmer.sql.ast.impl.table;

public interface TableAliasAllocator {

    String allocateTableAlias(TableLikeImplementor<?> owner);
}
