package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface TableProxy<E> extends Table<E> {

    Table<?> __parent();

    ImmutableProp __prop();

    TableImplementor<E> __unwrap();

    TableImplementor<E> __resolve(RootTableResolver resolver);

    <P extends TableProxy<E>> P __disableJoin(String reason);
}
