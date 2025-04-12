package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.OldChain;
import org.jetbrains.annotations.NotNull;

public interface NativeBuilder<T> extends NativeContext {

    @OldChain
    @NotNull
    @Override
    NativeBuilder<T> expression(@NotNull Expression<?> expression);

    @OldChain
    @NotNull
    @Override
    NativeBuilder<T> value(@NotNull Object value);

    @NotNull
    Expression<T> build();

    interface Str extends NativeBuilder<String> {

        @OldChain
        @Override
        @NotNull
        NativeBuilder<String> expression(@NotNull Expression<?> expression);

        @OldChain
        @Override
        @NotNull
        NativeBuilder<String> value(@NotNull Object value);

        @Override
        @NotNull
        StringExpression build();
    }

    interface Cmp<T extends Comparable<?>> extends NativeBuilder<T> {

        @OldChain
        @Override
        @NotNull
        Cmp<T> expression(@NotNull Expression<?> expression);

        @OldChain
        @Override
        @NotNull
        Cmp<T> value(@NotNull Object value);

        @Override
        @NotNull
        ComparableExpression<T> build();
    }

    interface Num<N extends Number & Comparable<N>> extends Cmp<N> {

        @OldChain
        @Override
        @NotNull
        Num<N> expression(@NotNull Expression<?> expression);

        @Override
        @NotNull
        Num<N> value(@NotNull Object value);

        @Override
        @NotNull
        NumericExpression<N> build();
    }

    interface Prd extends NativeBuilder<Boolean> {

        @OldChain
        @Override
        @NotNull
        Prd expression(@NotNull Expression<?> expression);

        @OldChain
        @Override
        @NotNull
        Prd value(@NotNull Object value);

        @Override
        @NotNull
        Predicate build();
    }
}
