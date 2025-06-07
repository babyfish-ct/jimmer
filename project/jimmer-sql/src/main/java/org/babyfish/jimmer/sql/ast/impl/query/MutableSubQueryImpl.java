package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.ExistsPredicate;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

public class MutableSubQueryImpl
        extends AbstractMutableQueryImpl
        implements MutableSubQuery {

    private AbstractMutableStatementImpl parent;

    private StatementContext ctx;

    private final Filter<?> filterOwner = FilterManager.currentFilter();

    public MutableSubQueryImpl(
            AbstractMutableStatementImpl parent,
            ImmutableType immutableType
    ) {
        super(parent.getSqlClient(), immutableType);
        StatementContext ctx = parent.getContext();
        if (ctx == null) {
            throw new IllegalStateException(
                    "The parent cannot be fluent statement whose context is not resolved"
            );
        }
        this.parent = parent;
        this.ctx = ctx;
    }

    public MutableSubQueryImpl(
            JSqlClientImplementor sqlClient,
            ImmutableType immutableType
    ) {
        super(sqlClient, immutableType);
    }

    public MutableSubQueryImpl(
            JSqlClientImplementor sqlClient,
            TableProxy<?> table
    ) {
        super(sqlClient, table);
    }

    public MutableSubQueryImpl(
            AbstractMutableStatementImpl parent,
            TableProxy<?> table
    ) {
        super(parent.getSqlClient(), table);
        StatementContext ctx = parent.getContext();
        this.parent = parent;
        this.ctx = ctx;
    }

    @Override
    public AbstractMutableStatementImpl getParent() {
        return parent;
    }

    @Override
    public StatementContext getContext() {
        return ctx;
    }

    @Override
    public MutableSubQueryImpl where(Predicate... predicates) {
        return (MutableSubQueryImpl) super.where(predicates);
    }

    /**
     * This method is deprecated, using {@code Dynamic Predicates}
     * is a more convenient approach.
     *
     * <p>Please look at this example:</p>
     * <pre>{@code
     * whereIf(name != null, table.name().eq(name))
     * }</pre>
     * When {@code name} is null, this code works because
     * {@code eq(null)} is automatically translated by Jimmer
     * to {@code isNull()}, which doesn't cause an exception.
     *
     * <p>Let's look at another example:</p>
     * <pre>{@code
     * whereIf(minPrice != null, table.price().ge(minPrice))
     * }</pre>
     * Except RUST marco, almost all programming languages
     * calculate all parameters first and then call the function.
     * Therefore, before {@code whereIf} is executed, {@code ge(null)}
     * already causes an exception.
     *
     * <p>For this reason, {@code whereIf} provides an overloaded form with a lambda parameter:</p>
     * <pre>{@code
     * whereIf(minPrice != null, () -> table.price().ge(minPrice))
     * }</pre>
     * Although this overloaded form can solve this problem,
     * it ultimately adds a mental burden during development.
     *
     * <p>Therefore, Jimmer provides {@code Dynamic Predicates}</p>
     *
     * <ul>
     *     <li>
     *         <b>Java</b>:
     *         eqIf, neIf, ltIf, leIf, gtIf, geIf, likeIf, ilikeIf, betweenIf
     *     </li>
     *     <li>
     *         <b>Kotlin</b>:
     *         eq?, ne?, lt?, le?, gt?, ge?, like?, ilike?, betweenIf?
     *     </li>
     * </ul>
     *
     * Taking Java's {@code geIf} as an example, this functionality
     * is ultimately implemented like this.
     * <pre>{@code
     * where(table.price().geIf(minPrice))
     * }</pre>
     */
    @OldChain
    @Override
    @Deprecated
    public MutableSubQueryImpl whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    @OldChain
    @Override
    public MutableSubQueryImpl whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

    @Override
    public MutableSubQueryImpl groupBy(Expression<?>... expressions) {
        return (MutableSubQueryImpl) super.groupBy(expressions);
    }

    @Override
    public MutableSubQueryImpl having(Predicate... predicates) {
        return (MutableSubQueryImpl) super.having(predicates);
    }

    @Override
    public MutableSubQueryImpl orderBy(Expression<?> ... expressions) {
        return (MutableSubQueryImpl) super.orderBy(expressions);
    }

    @Override
    public MutableSubQueryImpl orderByIf(boolean condition, Expression<?>... expressions) {
        return (MutableSubQueryImpl) super.orderByIf(condition, expressions);
    }

    @Override
    public MutableSubQueryImpl orderBy(Order... orders) {
        return (MutableSubQueryImpl) super.orderBy(orders);
    }

    @Override
    public MutableSubQueryImpl orderByIf(boolean condition, Order... orders) {
        return (MutableSubQueryImpl) super.orderByIf(condition, orders);
    }

    @Override
    public MutableSubQueryImpl orderBy(List<Order> orders) {
        return (MutableSubQueryImpl)super.orderBy(orders);
    }

    @Override
    public MutableSubQueryImpl orderByIf(boolean condition, List<Order> orders) {
        return (MutableSubQueryImpl)super.orderByIf(condition, orders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> ConfigurableSubQuery<R> select(Selection<R> selection) {
        if (selection instanceof FetcherSelection<?>) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return (ConfigurableSubQuery<R>) ConfigurableSubQueryImpl.of(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public ConfigurableSubQuery.Str select(StringExpression selection) {
        return (ConfigurableSubQuery.Str) ConfigurableSubQueryImpl.<String>of(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T extends Comparable<?>> ConfigurableSubQuery.Cmp<T> select(ComparableExpression<T> selection) {
        return (ConfigurableSubQuery.Cmp<T>) ConfigurableSubQueryImpl.<T>of(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <N extends Number & Comparable<N>> ConfigurableSubQuery.Num<N> select(NumericExpression<N> selection) {
        return (ConfigurableSubQuery.Num<N>) ConfigurableSubQueryImpl.<N>of(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T extends Date> ConfigurableSubQuery.Dt<T> select(DateExpression<T> selection) {
        return (ConfigurableSubQuery.Dt<T>) ConfigurableSubQueryImpl.<T>of(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T extends Temporal & Comparable<?>> ConfigurableSubQuery.Tp<T> select(TemporalExpression<T> selection) {
        return (ConfigurableSubQuery.Tp<T>) ConfigurableSubQueryImpl.<T>of(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T1, T2> ConfigurableSubQuery<Tuple2<T1, T2>> select(Selection<T1> selection1, Selection<T2> selection2) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(selection1, selection2)
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3> ConfigurableSubQuery<Tuple3<T1, T2, T3>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
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
    public <T1, T2, T3, T4> ConfigurableSubQuery<Tuple4<T1, T2, T3, T4>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5> ConfigurableSubQuery<Tuple5<T1, T2, T3, T4, T5>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?> ||
                selection5 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6> ConfigurableSubQuery<Tuple6<T1, T2, T3, T4, T5, T6>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?> ||
                selection5 instanceof FetcherSelection<?> ||
                selection6 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableSubQuery<Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?> ||
                selection5 instanceof FetcherSelection<?> ||
                selection6 instanceof FetcherSelection<?> ||
                selection7 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableSubQuery<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?> ||
                selection5 instanceof FetcherSelection<?> ||
                selection6 instanceof FetcherSelection<?> ||
                selection7 instanceof FetcherSelection<?> ||
                selection8 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableSubQuery<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8, Selection<T9> selection9) {
        if (selection1 instanceof FetcherSelection<?> ||
            selection2 instanceof FetcherSelection<?> ||
            selection3 instanceof FetcherSelection<?> ||
            selection4 instanceof FetcherSelection<?> ||
            selection5 instanceof FetcherSelection<?> ||
            selection6 instanceof FetcherSelection<?> ||
            selection7 instanceof FetcherSelection<?> ||
            selection8 instanceof FetcherSelection<?> ||
            selection9 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
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

    @Override
    public Predicate exists() {
        return ExistsPredicate.of(this, false);
    }

    @Override
    public Predicate notExists() {
        return ExistsPredicate.of(this, true);
    }

    public void setParent(AbstractMutableStatementImpl parent) {
        if (this.parent == null) {
            this.parent = parent;
            ctx = parent.getContext();
        } else if (!MutableDeleteImpl.isCompatible(this.parent, parent)) {
            throw new IllegalStateException(
                    "The sub query cannot be added to parent query \"" +
                            parent +
                            "\" because it is belong to another parent query \"" +
                            this.parent +
                            "\""
            );
        }
    }

    @Override
    public void resolveVirtualPredicate(AstContext ctx) {
        setParent(ctx.getStatement());
        super.resolveVirtualPredicate(ctx);
    }

    public Filter<?> filterOwner() {
        return filterOwner;
    }
}

