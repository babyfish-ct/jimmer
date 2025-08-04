package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.impl.query.MergedTypedRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.query.TypedRootQueryImplementor;
import org.babyfish.jimmer.sql.exception.EmptyResultException;
import org.babyfish.jimmer.sql.exception.TooManyResultsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TypedRootQuery<R> extends Executable<List<R>> {

    @SafeVarargs
    static <R> TypedRootQuery<R> union(TypedRootQuery<R> ... queries) {
        return MergedTypedRootQueryImpl.of("union", queries);
    }

    @SafeVarargs
    static <R> TypedRootQuery<R> unionAll(TypedRootQuery<R> ... queries) {
        return MergedTypedRootQueryImpl.of("union all", queries);
    }

    @SafeVarargs
    static <R> TypedRootQuery<R> minus(TypedRootQuery<R> ... queries) {
        return MergedTypedRootQueryImpl.of("minus", queries);
    }

    @SafeVarargs
    static <R> TypedRootQuery<R> intersect(TypedRootQuery<R> ... queries) {
        return MergedTypedRootQueryImpl.of("intersect", queries);
    }

    /**
     * @deprecated Please view the static method {@link #union(TypedRootQuery[])}
     */
    @Deprecated
    default TypedRootQuery<R> union(TypedRootQuery<R> other) {
        return TypedRootQuery.union(this, other);
    }

    /**
     * @deprecated Please view the static method {@link #unionAll(TypedRootQuery[])}
     */
    @Deprecated
    default TypedRootQuery<R> unionAll(TypedRootQuery<R> other) {
        return TypedRootQuery.unionAll(this, other);
    }

    /**
     * @deprecated Please view the static method {@link #minus(TypedRootQuery[])}
     */
    @Deprecated
    default TypedRootQuery<R> minus(TypedRootQuery<R> other) {
        return TypedRootQuery.minus(this, other);
    }

    /**
     * @deprecated Please view the static method {@link #intersect(TypedRootQuery[])}
     */
    @Deprecated
    default TypedRootQuery<R> intersect(TypedRootQuery<R> other) {
        return TypedRootQuery.intersect(this, other);
    }

    default R fetchOne() {
        return fetchOne(null);
    }

    default R fetchOne(Connection con) {
        List<R> list = this instanceof TypedRootQueryImplementor<?> ?
                ((TypedRootQueryImplementor<R>) this).withLimit(2).execute(con) :
                execute(con);
        if (list.isEmpty()) {
            throw new EmptyResultException();
        }
        if (list.size() > 1) {
            throw new TooManyResultsException();
        }
        return list.get(0);
    }

    @Nullable
    default R fetchOneOrNull() {
        return fetchOneOrNull(null);
    }

    @Nullable
    default R fetchOneOrNull(Connection con) {
        List<R> list = this instanceof TypedRootQueryImplementor<?> ?
                ((TypedRootQueryImplementor<R>) this).withLimit(2).execute(con) :
                execute(con);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new TooManyResultsException();
        }
        return list.get(0);
    }

    default R fetchFirst() {
        return fetchFirst(null);
    }

    default R fetchFirst(Connection con) {
        List<R> list = this instanceof TypedRootQueryImplementor<?> ?
                ((TypedRootQueryImplementor<R>) this).withLimit(1).execute(con) :
                execute(con);
        if (list.isEmpty()) {
            throw new EmptyResultException();
        }
        return list.get(0);
    }

    @Nullable
    default R fetchFirstOrNull() {
        return fetchFirstOrNull(null);
    }

    @Nullable
    default R fetchFirstOrNull(Connection con) {
        List<R> list = this instanceof TypedRootQueryImplementor<?> ?
                ((TypedRootQueryImplementor<R>) this).withLimit(1).execute(con) :
                execute(con);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @NotNull
    default Optional<R> fetchOptional() {
        return Optional.ofNullable(fetchOneOrNull());
    }

    @NotNull
    default Optional<R> fetchOptional(Connection con) {
        return Optional.ofNullable(fetchOneOrNull(con));
    }

    default <X> List<X> map(Function<R, X> mapper) {
        return map(null, mapper);
    }

    <X> List<X> map(Connection con, Function<R, X> mapper);

    default void forEach(Consumer<R> consumer) {
        forEach(null, -1, consumer);
    }

    default void forEach(Connection con, Consumer<R> consumer) {
        forEach(con, -1, consumer);
    }

    default void forEach(int batchSize, Consumer<R> consumer) {
        forEach(null, batchSize, consumer);
    }

    void forEach(Connection con, int batchSize, Consumer<R> consumer);
}
