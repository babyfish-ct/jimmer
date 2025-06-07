package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.Date;

public interface ConfigurableBaseQuery<T extends BaseTable> extends TypedBaseQuery<T> {

    @NewChain
    ConfigurableBaseQuery<T> distinct();

    @NewChain
    ConfigurableBaseQuery<T> limit(int limit);

    @NewChain
    ConfigurableBaseQuery<T> offset(long offset);

    @NewChain
    ConfigurableBaseQuery<T> limit(int limit, long offset);

    @NewChain
    ConfigurableBaseQuery<T> hint(@Nullable String hint);

    interface Simple1<S1 extends Selection<?>> extends
            ConfigurableBaseQuery<BaseTable1<S1>> {

        <T extends Table<?>> Simple2<S1, T> addSelect(T table);

        <T extends AbstractTypedEmbeddedPropExpression<?>> Simple2<S1, T> addSelect(T expr);

        <V> Simple2<S1, Expression<V>> addSelect(Expression<V> expr);

        Simple2<S1, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Simple2<S1, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Simple2<S1, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Simple2<S1, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Simple2<S1, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Simple2<S1 extends Selection<?>, S2 extends Selection<?>>
            extends ConfigurableBaseQuery<BaseTable2<S1, S2>> {

        <T extends Table<?>> Simple3<S1, S2, T> addSelect(T table);

        <T extends AbstractTypedEmbeddedPropExpression<?>> Simple3<S1, S2, T> addSelect(T expr);

        <V> Simple3<S1, S2, Expression<V>> addSelect(Expression<V> expr);

        Simple3<S1, S2, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Simple3<S1, S2, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Simple3<S1, S2, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Simple3<S1, S2, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Simple3<S1, S2, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Simple3<S1 extends Selection<?>, S2 extends Selection<?>, S3 extends Selection<?>>
            extends ConfigurableBaseQuery<BaseTable3<S1, S2, S3>> {

    }
}
