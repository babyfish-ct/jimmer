package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface TableProxy<E> extends Table<E> {

    TableImplementor<E> __unwrap();

    TableImplementor<E> __resolve(RootTableResolver resolver);

    String __joinDisabledReason();
}
