package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface TableWrapper<E> extends Table<E> {

    TableImplementor<E> unwrap();

    String getJoinDisabledReason();
}
