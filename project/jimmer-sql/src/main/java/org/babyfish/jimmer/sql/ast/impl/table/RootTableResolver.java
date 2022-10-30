package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.table.Table;

public interface RootTableResolver {

    <E> TableImplementor<E> resolveRootTable(Table<E> table);
}
