package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.*;
import org.babyfish.jimmer.sql.ast.table.spi.TableExProxy;
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

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Simple2<S1, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

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

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Simple3<S1, S2, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

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

        <T extends Table<?>> Simple4<S1, S2, S3, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Simple4<S1, S2, S3, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Simple4<S1, S2, S3, T> addSelect(T expr);

        <V> Simple4<S1, S2, S3, Expression<V>> addSelect(Expression<V> expr);

        Simple4<S1, S2, S3, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Simple4<S1, S2, S3, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Simple4<S1, S2, S3, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Simple4<S1, S2, S3, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Simple4<S1, S2, S3, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Simple4<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable4<S1, S2, S3, S4>> {

        <T extends Table<?>> Simple5<S1, S2, S3, S4, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Simple5<S1, S2, S3, S4, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Simple5<S1, S2, S3, S4, T> addSelect(T expr);

        <V> Simple5<S1, S2, S3, S4, Expression<V>> addSelect(Expression<V> expr);

        Simple5<S1, S2, S3, S4, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Simple5<S1, S2, S3, S4, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Simple5<S1, S2, S3, S4, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Simple5<S1, S2, S3, S4, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Simple5<S1, S2, S3, S4, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Simple5<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable5<S1, S2, S3, S4, S5>> {

        <T extends Table<?>> Simple6<S1, S2, S3, S4, S5, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Simple6<S1, S2, S3, S4, S5, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Simple6<S1, S2, S3, S4, S5, T> addSelect(T expr);

        <V> Simple6<S1, S2, S3, S4, S5, Expression<V>> addSelect(Expression<V> expr);

        Simple6<S1, S2, S3, S4, S5, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Simple6<S1, S2, S3, S4, S5, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Simple6<S1, S2, S3, S4, S5, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Simple6<S1, S2, S3, S4, S5, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Simple6<S1, S2, S3, S4, S5, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Simple6<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable6<S1, S2, S3, S4, S5, S6>> {

        <T extends Table<?>> Simple7<S1, S2, S3, S4, S5, S6, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Simple7<S1, S2, S3, S4, S5, S6, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Simple7<S1, S2, S3, S4, S5, S6, T> addSelect(T expr);

        <V> Simple7<S1, S2, S3, S4, S5, S6, Expression<V>> addSelect(Expression<V> expr);

        Simple7<S1, S2, S3, S4, S5, S6, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Simple7<S1, S2, S3, S4, S5, S6, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Simple7<S1, S2, S3, S4, S5, S6, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Simple7<S1, S2, S3, S4, S5, S6, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Simple7<S1, S2, S3, S4, S5, S6, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Simple7<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable7<S1, S2, S3, S4, S5, S6, S7>> {

        <T extends Table<?>> Simple8<S1, S2, S3, S4, S5, S6, S7, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Simple8<S1, S2, S3, S4, S5, S6, S7, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Simple8<S1, S2, S3, S4, S5, S6, S7, T> addSelect(T expr);

        <V> Simple8<S1, S2, S3, S4, S5, S6, S7, Expression<V>> addSelect(Expression<V> expr);

        Simple8<S1, S2, S3, S4, S5, S6, S7, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Simple8<S1, S2, S3, S4, S5, S6, S7, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Simple8<S1, S2, S3, S4, S5, S6, S7, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Simple8<S1, S2, S3, S4, S5, S6, S7, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Simple8<S1, S2, S3, S4, S5, S6, S7, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Simple8<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable8<S1, S2, S3, S4, S5, S6, S7, S8>> {

        <T extends Table<?>> Simple9<S1, S2, S3, S4, S5, S6, S7, S8, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Simple9<S1, S2, S3, S4, S5, S6, S7, S8, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Simple9<S1, S2, S3, S4, S5, S6, S7, S8, T> addSelect(T expr);

        <V> Simple9<S1, S2, S3, S4, S5, S6, S7, S8, Expression<V>> addSelect(Expression<V> expr);

        Simple9<S1, S2, S3, S4, S5, S6, S7, S8, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Simple9<S1, S2, S3, S4, S5, S6, S7, S8, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Simple9<S1, S2, S3, S4, S5, S6, S7, S8, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Simple9<S1, S2, S3, S4, S5, S6, S7, S8, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Simple9<S1, S2, S3, S4, S5, S6, S7, S8, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Simple9<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>,
            S9 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable9<S1, S2, S3, S4, S5, S6, S7, S8, S9>> {

    }
}
