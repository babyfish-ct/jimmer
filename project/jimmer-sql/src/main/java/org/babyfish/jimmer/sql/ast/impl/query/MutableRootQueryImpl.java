package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

public class MutableRootQueryImpl<T extends Table<?>>
        extends AbstractMutableQueryImpl
        implements MutableRootQuery<T> {

    private final StatementContext ctx;

    public MutableRootQueryImpl(
            JSqlClient sqlClient,
            ImmutableType immutableType,
            ExecutionPurpose purpose,
            boolean ignoreFilter
    ) {
        super(sqlClient, immutableType);
        ctx = new StatementContext(purpose, ignoreFilter);
    }

    public MutableRootQueryImpl(
            JSqlClient sqlClient,
            TableProxy<?> table,
            ExecutionPurpose purpose,
            boolean ignoreFilter
    ) {
        super(sqlClient, table);
        ctx = new StatementContext(purpose, ignoreFilter);
    }

    @SuppressWarnings("unchecked")
    public MutableRootQueryImpl(
            StatementContext ctx,
            JSqlClient sqlClient,
            ImmutableType immutableType
    ) {
        super(sqlClient, immutableType);
        this.ctx = ctx;
    }

    public MutableRootQueryImpl(
            StatementContext ctx,
            JSqlClient sqlClient,
            TableProxy<?> table
    ) {
        super(sqlClient, table);
        this.ctx = ctx;
    }

    @Override
    public AbstractMutableStatementImpl getParent() {
        return null;
    }

    @Override
    public StatementContext getContext() {
        return ctx;
    }

    @Override
    public <R> ConfigurableRootQuery<T, R> select(Selection<R> selection) {
        freeze();
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T1, T2> ConfigurableRootQuery<T, Tuple2<T1, T2>> select(Selection<T1> selection1, Selection<T2> selection2) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(Arrays.asList(selection1, selection2)),
                this
        );
    }

    @Override
    public <T1, T2, T3> ConfigurableRootQuery<T, Tuple3<T1, T2, T3>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4> ConfigurableRootQuery<T, Tuple4<T1, T2, T3, T4>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5> ConfigurableRootQuery<T, Tuple5<T1, T2, T3, T4, T5>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> ConfigurableRootQuery<T, Tuple6<T1, T2, T3, T4, T5, T6>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5,
                                selection6
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableRootQuery<T, Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5,
                                selection6,
                                selection7
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableRootQuery<T, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5,
                                selection6,
                                selection7,
                                selection8
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableRootQuery<T, Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8, Selection<T9> selection9) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5,
                                selection6,
                                selection7,
                                selection8,
                                selection9
                        )
                ),
                this
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> where(Predicate... predicates) {
        return (MutableRootQueryImpl<T>) super.where(predicates);
    }

    @OldChain
    @Override
    public MutableRootQueryImpl<T> whereIf(boolean condition, Predicate predicates) {
        if (condition) {
            where(predicates);
        }
        return this;
    }

    @OldChain
    @Override
    public MutableRootQueryImpl<T> whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> groupBy(Expression<?>... expressions) {
        return (MutableRootQueryImpl<T>) super.groupBy(expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> having(Predicate... predicates) {
        return (MutableRootQueryImpl<T>) super.having(predicates);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> orderBy(Expression<?> ... expressions) {
        return (MutableRootQueryImpl<T>) super.orderBy(expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> orderByIf(boolean condition, Expression<?>... expressions) {
        return (MutableRootQueryImpl<T>) super.orderByIf(condition, expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> orderBy(Order... orders) {
        return (MutableRootQueryImpl<T>) super.orderBy(orders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> orderByIf(boolean condition, Order... orders) {
        return (MutableRootQueryImpl<T>)super.orderByIf(condition, orders);
    }

    @Override
    protected void onFrozen() {
        Filter<Props> filter = getSqlClient().getFilters().getFilter(
                getTable().getImmutableType(),
                getContext().isFilterIgnored()
        );
        if (filter instanceof CacheableFilter<?>) {
            disableSubQuery();
            try {
                filter.filter(
                        new FilterArgsImpl<>(this, this.getTable(), true)
                );
            } finally {
                enableSubQuery();
            }
        } else if (filter != null) {
            filter.filter(
                    new FilterArgsImpl<>(this, this.getTable(), false)
            );
        }
        super.onFrozen();
    }
}
