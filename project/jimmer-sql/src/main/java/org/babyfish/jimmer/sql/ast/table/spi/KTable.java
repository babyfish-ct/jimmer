package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;

public interface KTable<E> {

    TableImplementor<E> getImplementor();
}
