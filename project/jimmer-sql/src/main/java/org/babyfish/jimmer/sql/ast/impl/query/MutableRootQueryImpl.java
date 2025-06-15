package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.query.specification.PredicateApplier;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.ast.query.specification.SpecificationArgs;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class MutableRootQueryImpl<T extends TableLike<?>>
        extends AbstractMutableQueryImpl
        implements MutableRootQuery<T> {

    private final StatementContext ctx;

    public MutableRootQueryImpl(
            JSqlClientImplementor sqlClient,
            ImmutableType immutableType,
            ExecutionPurpose purpose,
            FilterLevel filterLevel
    ) {
        super(sqlClient, immutableType);
        ctx = new StatementContext(purpose, filterLevel);
        getTableLikeImplementor();
    }

    public MutableRootQueryImpl(
            JSqlClientImplementor sqlClient,
            TableProxy<?> table,
            ExecutionPurpose purpose,
            FilterLevel filterLevel
    ) {
        super(sqlClient, table);
        ctx = new StatementContext(purpose, filterLevel);
    }

    public MutableRootQueryImpl(
            JSqlClientImplementor sqlClient,
            BaseTable table,
            ExecutionPurpose purpose,
            FilterLevel filterLevel
    ) {
        super(sqlClient, table);
        ctx = new StatementContext(purpose, filterLevel);
    }

    public MutableRootQueryImpl(
            StatementContext ctx,
            JSqlClientImplementor sqlClient,
            ImmutableType immutableType
    ) {
        super(sqlClient, immutableType);
        this.ctx = ctx;
    }

    public MutableRootQueryImpl(
            StatementContext ctx,
            JSqlClientImplementor sqlClient,
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

    @Override
    public MutableRootQuery<T> where(Specification<?> specification) {
        if (specification == null) {
            return this;
        }
        if (!(specification instanceof JSpecification<?, ?>)) {
            throw new IllegalArgumentException(
                    "The specification must be instance of \"" +
                            JSpecification.class.getName() +
                            "\""
            );
        }
        return where((JSpecification<?, T>) specification);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQuery<T> where(JSpecification<?, T> specification) {
        if (specification != null) {
            SpecificationArgs<Object, Table<Object>> args =
                    new SpecificationArgs<>(new PredicateApplier(this));
            JSpecification<Object, Table<Object>> implementor =
                    (JSpecification<Object, Table<Object>>) specification;
            implementor.applyTo(args);
        }
        return this;
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
    public MutableRootQueryImpl<T> whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
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

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> orderBy(List<Order> orders) {
        return (MutableRootQueryImpl<T>)super.orderBy(orders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> orderByIf(boolean condition, List<Order> orders) {
        return (MutableRootQueryImpl<T>)super.orderByIf(condition, orders);
    }

    @Override
    public boolean exists(Connection con) {
        return !select(Expression.constant(1))
                .limit(1)
                .execute()
                .isEmpty();
    }

    @Override
    public String toString() {
        if (getTable() instanceof BaseTable) {
            return "MutableRootQuery{baseTable=" +
                    getTable() +
                    "}";
        }
        return "MutableRootQuery{type=" +
                getType() +
                "}";
    }
}
