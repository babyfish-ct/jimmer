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

    interface Query1<S1 extends Selection<?>> extends
            ConfigurableBaseQuery<BaseTable1<S1>> {

        <T extends Table<?>> Query2<S1, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Query2<S1, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Query2<S1, T> addSelect(T expr);

        <V> Query2<S1, Expression<V>> addSelect(Expression<V> expr);

        Query2<S1, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Query2<S1, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Query2<S1, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Query2<S1, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Query2<S1, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Query2<S1 extends Selection<?>, S2 extends Selection<?>>
            extends ConfigurableBaseQuery<BaseTable2<S1, S2>> {

        <T extends Table<?>> Query3<S1, S2, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Query3<S1, S2, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Query3<S1, S2, T> addSelect(T expr);

        <V> Query3<S1, S2, Expression<V>> addSelect(Expression<V> expr);

        Query3<S1, S2, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Query3<S1, S2, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Query3<S1, S2, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Query3<S1, S2, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Query3<S1, S2, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Query3<S1 extends Selection<?>, S2 extends Selection<?>, S3 extends Selection<?>>
            extends ConfigurableBaseQuery<BaseTable3<S1, S2, S3>> {

        <T extends Table<?>> Query4<S1, S2, S3, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Query4<S1, S2, S3, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Query4<S1, S2, S3, T> addSelect(T expr);

        <V> Query4<S1, S2, S3, Expression<V>> addSelect(Expression<V> expr);

        Query4<S1, S2, S3, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Query4<S1, S2, S3, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Query4<S1, S2, S3, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Query4<S1, S2, S3, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Query4<S1, S2, S3, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Query4<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable4<S1, S2, S3, S4>> {

        <T extends Table<?>> Query5<S1, S2, S3, S4, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Query5<S1, S2, S3, S4, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Query5<S1, S2, S3, S4, T> addSelect(T expr);

        <V> Query5<S1, S2, S3, S4, Expression<V>> addSelect(Expression<V> expr);

        Query5<S1, S2, S3, S4, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Query5<S1, S2, S3, S4, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Query5<S1, S2, S3, S4, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Query5<S1, S2, S3, S4, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Query5<S1, S2, S3, S4, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Query5<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable5<S1, S2, S3, S4, S5>> {

        <T extends Table<?>> Query6<S1, S2, S3, S4, S5, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Query6<S1, S2, S3, S4, S5, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Query6<S1, S2, S3, S4, S5, T> addSelect(T expr);

        <V> Query6<S1, S2, S3, S4, S5, Expression<V>> addSelect(Expression<V> expr);

        Query6<S1, S2, S3, S4, S5, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Query6<S1, S2, S3, S4, S5, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Query6<S1, S2, S3, S4, S5, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Query6<S1, S2, S3, S4, S5, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Query6<S1, S2, S3, S4, S5, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Query6<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable6<S1, S2, S3, S4, S5, S6>> {

        <T extends Table<?>> Query7<S1, S2, S3, S4, S5, S6, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Query7<S1, S2, S3, S4, S5, S6, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Query7<S1, S2, S3, S4, S5, S6, T> addSelect(T expr);

        <V> Query7<S1, S2, S3, S4, S5, S6, Expression<V>> addSelect(Expression<V> expr);

        Query7<S1, S2, S3, S4, S5, S6, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Query7<S1, S2, S3, S4, S5, S6, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Query7<S1, S2, S3, S4, S5, S6, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Query7<S1, S2, S3, S4, S5, S6, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Query7<S1, S2, S3, S4, S5, S6, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Query7<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable7<S1, S2, S3, S4, S5, S6, S7>> {

        <T extends Table<?>> Query8<S1, S2, S3, S4, S5, S6, S7, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Query8<S1, S2, S3, S4, S5, S6, S7, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Query8<S1, S2, S3, S4, S5, S6, S7, T> addSelect(T expr);

        <V> Query8<S1, S2, S3, S4, S5, S6, S7, Expression<V>> addSelect(Expression<V> expr);

        Query8<S1, S2, S3, S4, S5, S6, S7, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Query8<S1, S2, S3, S4, S5, S6, S7, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Query8<S1, S2, S3, S4, S5, S6, S7, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Query8<S1, S2, S3, S4, S5, S6, S7, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Query8<S1, S2, S3, S4, S5, S6, S7, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Query8<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>
    > extends ConfigurableBaseQuery<BaseTable8<S1, S2, S3, S4, S5, S6, S7, S8>> {

        <T extends Table<?>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, T> addSelect(T table);

        @SuppressWarnings("unchecked")
        default <T extends Table<?>, TEX extends TableExProxy<?, T>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, T> addSelect(TEX table) {
            return addSelect((T)table);
        }

        <T extends AbstractTypedEmbeddedPropExpression<?>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, T> addSelect(T expr);

        <V> Query9<S1, S2, S3, S4, S5, S6, S7, S8, Expression<V>> addSelect(Expression<V> expr);

        Query9<S1, S2, S3, S4, S5, S6, S7, S8, StringExpression> addSelect(StringExpression expr);

        <V extends Number & Comparable<V>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, NumericExpression<V>> addSelect(NumericExpression<V> expr);

        <V extends Comparable<?>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

        <V extends Date> Query9<S1, S2, S3, S4, S5, S6, S7, S8, DateExpression<V>> addSelect(DateExpression<V> expr);

        <V extends Temporal & Comparable<?>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
    }

    interface Query9<
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
