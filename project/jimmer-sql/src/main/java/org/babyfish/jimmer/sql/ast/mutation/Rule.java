package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Rule {

    static <E, V> Dsl.InsertBuilder<E, V> insert(TypedProp.Scalar<E, V> prop) {
        throw new UnsupportedOperationException();
    }

    static <E, X> Dsl.InsertPathBuilder<E, X> insert(TypedProp.Single<E, X> prop) {
        return null;
    }

    static <E, V> Dsl.UpdateTableBuilder<E, V> update(TypedProp.Scalar<E, V> prop) {
        throw new UnsupportedOperationException();
    }

    static <E, X> Dsl.UpdatePathBuilder<E, X> update(TypedProp.Embedded<E, X> prop) {
        return null;
    }

    static <E, X> Dsl.UpdatePathBuilder<E, X> update(TypedProp.Reference<E, X> prop) {
        return null;
    }

    static <E, T extends Table<E>> Dsl.UpdateFilterBuilder<E, T> update(Class<T> tableType) {
        return null;
    }

    interface Dsl {

        interface InsertPathBuilder<E, X> {
            <T> InsertBuilder<E, T> dot(TypedProp.Scalar<E, T> prop);
            <Y> InsertPathBuilder<E, Y> dot(TypedProp.Single<E, Y> prop);
        }

        interface InsertBuilder<E, V> {
            Rule unconditionally(Supplier<Expression<V>> block);
            Rule unconditionally(V value);
            Rule whenUnloaded(Supplier<Expression<V>> block);
            Rule whenUnloaded(V value);
            Rule whenLoaded(Function<ColumnValueHolder<E, V>, Expression<V>> block);
            Rule never();
        }

        interface UpdateTableBuilder<E, V> {
            <T extends Table<E>> UpdateBuilder<E, V, T> of(Class<T> tableType);
        }

        interface UpdatePathBuilder<E, X> {
            <T> UpdateTableBuilder<E, T> dot(TypedProp.Scalar<X, T> prop);
            <T> UpdatePathBuilder<E, T> dot(TypedProp.Embedded<X, T> prop);
            <T> UpdatePathBuilder<E, T> dot(TypedProp.Reference<X, T> prop);
        }

        interface UpdateBuilder<E, V, T extends Table<E>> {
            Rule unconditionally(Function<UpdateDefaultValueContext<E, T>, Expression<V>> block);
            Rule unconditionally(V value);
            Rule whenUnloaded(Function<UpdateDefaultValueContext<E, T>, Expression<V>> block);
            Rule whenUnloaded(V value);
            Rule whenLoaded(Function<UpdateValueContext<E, V, T>, Expression<V>> block);
            Rule never();
        }

        interface UpdateDefaultValueContext<E, T extends Table<E>> {
            T table();
        }

        interface UpdateValueContext<E, V, T extends Table<E>> extends UpdateDefaultValueContext<E, T>, ColumnValueHolder<E, V> {
        }

        interface UpdateFilterBuilder<E, T extends Table<E>> {
            Rule where(Function<UpdateFilterContext<E, T>, Predicate> block);
        }

        interface UpdateFilterContext<E, T extends Table<E>> extends UpdateDefaultValueContext<E, T>, ValueHolder<E> {
        }

        interface ValueHolder<E> {
            boolean isLoaded(TypedProp.Single<E, ?> prop);
            <N extends Number & Comparable<N>> NumericExpression<N> value(TypedProp.NumericScalar<E, N> prop);
            <T extends Comparable<?>> ComparableExpression<T> value(TypedProp.ComparableScalar<E, T> prop);
            <T> Expression<T> value(TypedProp.Scalar<E, T> prop);
            StringExpression value(TypedProp.StringScalar<E> prop, String defaultValue);
            <N extends Number & Comparable<N>> NumericExpression<N> value(TypedProp.NumericScalar<E, N> prop, N defaultValue);
            <T extends Comparable<?>> ComparableExpression<T> value(TypedProp.ComparableScalar<E, T> prop, T defaultValue);
            <T> Expression<T> value(TypedProp.Scalar<E, T> prop, T defaultValue);
        }

        interface ColumnValueHolder<E, V> extends ValueHolder<E> {
            V newValue();
        }
    }
}
