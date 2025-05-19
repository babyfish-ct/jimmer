package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.Slice;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.function.BiFunction;

public interface ConfigurableRootQuery<T extends TableLike<?>, R> extends TypedRootQuery<R> {

    /**
     * Ignore the sorting and pagination settings of the current query, 
     * query the total number of data before pagination
     *
     * <p>
     *     In general, users do not need to directly use this method, 
     *      but call the {@link #fetchPage(int, int)} method instead
     * </p>
     * 
     * @return Total row count before pagination
     */
    default long fetchUnlimitedCount() {
        return fetchUnlimitedCount(null);
    }

    /**
     * Ignore the sorting and pagination settings of the current query, 
     * query the total number of data before pagination
     *
     * <p>
     *     In general, users do not need to directly use this method, 
     *      but call the {@link #fetchPage(int, int, Connection)} method instead
     * </p>
     *
     * @return Total row count before pagination
     */
    default long fetchUnlimitedCount(Connection con) {
        return reselect((q, t) -> q.select(Expression.rowCount()))
            .withoutSortingAndPaging()
            .execute(con)
            .get(0);
    }

    default boolean exists() {
        return exists(null);
    }

    default boolean exists(Connection con) {
        return !limit(1, 0L)
                .reselect((q, t) -> q.select(Expression.constant(1)))
                .execute(con)
                .isEmpty();
    }

    @NotNull
    default <P> P fetchPage(int pageIndex, int pageSize, PageFactory<R, P> pageFactory) {
        return fetchPage(pageIndex, pageSize, null, pageFactory);
    }

    @NotNull
    <P> P fetchPage(int pageIndex, int pageSize, Connection con, PageFactory<R, P> pageFactory);

    @NotNull
    default Page<R> fetchPage(int pageIndex, int pageSize) {
        return fetchPage(pageIndex, pageSize, null, PageFactory.standard());
    }

    @NotNull
    default Page<R> fetchPage(int pageIndex, int pageSize, Connection con) {
        return fetchPage(pageIndex, pageSize, con, PageFactory.standard());
    }

    Slice<R> fetchSlice(int limit, int offset, @Nullable Connection con);

    default Slice<R> fetchSlice(int limit, int offset) {
        return fetchSlice(limit, offset, null);
    }

    @NewChain
    <X> ConfigurableRootQuery<T, X> reselect(
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, X>> block
    );

    @NewChain
    ConfigurableRootQuery<T, R> distinct();

    @NewChain
    ConfigurableRootQuery<T, R> limit(int limit);

    @NewChain
    ConfigurableRootQuery<T, R> offset(long offset);

    @NewChain
    ConfigurableRootQuery<T, R> limit(int limit, long offset);

    @NewChain
    ConfigurableRootQuery<T, R> withoutSortingAndPaging();

    /**
     * @return If the original query does not have `order by` clause, returns null
     */
    @NewChain
    @Nullable
    ConfigurableRootQuery<T, R> reverseSorting();

    @NewChain
    ConfigurableRootQuery<T, R> setReverseSortOptimizationEnabled(boolean enabled);

    @NewChain
    default ConfigurableRootQuery<T, R> forUpdate() {
        return forUpdate(true);
    }

    @NewChain
    ConfigurableRootQuery<T, R> forUpdate(boolean forUpdate);

    /**
     * Set the hint
     * @param hint Optional hint, both <b>/&#42;+ sth &#42;/</b> and <b>sth</b> are OK.
     * @return A new query object
     */
    @NewChain
    ConfigurableRootQuery<T, R> hint(@Nullable String hint);
}
