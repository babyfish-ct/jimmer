package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface TableProxy<E> extends Table<E> {

    Table<?> __parent();

    ImmutableProp __prop();

    WeakJoinHandle __weakJoinHandle();

    boolean __isInverse();

    TableImplementor<E> __unwrap();

    TableImplementor<E> __resolve(RootTableResolver resolver);

    <P extends TableProxy<E>> P __disableJoin(String reason);

    TableProxy<E> __baseTableOwner(BaseTableOwner baseTableOwner);

    @Nullable
    BaseTableOwner __baseTableOwner();

    JoinType __joinType();
}
