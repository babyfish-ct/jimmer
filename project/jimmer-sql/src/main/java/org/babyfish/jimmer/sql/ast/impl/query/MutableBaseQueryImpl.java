package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.query.MutableBaseQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

public class MutableBaseQueryImpl extends AbstractMutableQueryImpl implements MutableBaseQuery {

    private AbstractMutableStatementImpl parent;

    private StatementContext ctx;

    public MutableBaseQueryImpl(
            JSqlClientImplementor sqlClient,
            TableProxy<?> table
    ) {
        super(sqlClient, table);
    }

    public MutableBaseQueryImpl(
            JSqlClientImplementor sqlClient,
            ImmutableType immutableType
    ) {
        super(sqlClient, immutableType);
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
    public MutableBaseQueryImpl where(Predicate... predicates) {
        return (MutableBaseQueryImpl) super.where(predicates);
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
    public MutableBaseQueryImpl whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    @OldChain
    @Override
    public MutableBaseQueryImpl whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

    @Override
    public MutableBaseQueryImpl groupBy(Expression<?>... expressions) {
        return (MutableBaseQueryImpl) super.groupBy(expressions);
    }

    @Override
    public MutableBaseQueryImpl having(Predicate... predicates) {
        return (MutableBaseQueryImpl) super.having(predicates);
    }

    @Override
    public MutableBaseQueryImpl orderBy(Expression<?> ... expressions) {
        return (MutableBaseQueryImpl) super.orderBy(expressions);
    }

    @Override
    public MutableBaseQueryImpl orderByIf(boolean condition, Expression<?>... expressions) {
        return (MutableBaseQueryImpl) super.orderByIf(condition, expressions);
    }

    @Override
    public MutableBaseQueryImpl orderBy(Order... orders) {
        return (MutableBaseQueryImpl) super.orderBy(orders);
    }

    @Override
    public MutableBaseQueryImpl orderByIf(boolean condition, Order... orders) {
        return (MutableBaseQueryImpl) super.orderByIf(condition, orders);
    }

    @Override
    public MutableBaseQueryImpl orderBy(List<Order> orders) {
        return (MutableBaseQueryImpl) super.orderBy(orders);
    }

    @Override
    public MutableBaseQueryImpl orderByIf(boolean condition, List<Order> orders) {
        return (MutableBaseQueryImpl)super.orderByIf(condition, orders);
    }

    public void setParent(AbstractMutableStatementImpl parent) {
        if (this.parent == null) {
            this.parent = parent;
            ctx = parent.getContext();
        } else if (parent instanceof MutableBaseQueryImpl) {
            if (ctx != parent.getContext()) {
                throw new IllegalStateException(
                        "The recursive base query cannot be added to statement context"
                );
            }
        } else if (!MutableDeleteImpl.isCompatible(this.parent, parent)) {
            throw new IllegalStateException(
                    "The base query cannot be added to parent query because it is belong to another parent query"
            );
        }
    }

    @Override
    public void resolveVirtualPredicate(AstContext ctx) {
        setParent(ctx.getStatement());
        super.resolveVirtualPredicate(ctx);
    }

    @Override
    public <T extends Table<?>> ConfigurableBaseQuery.Query1<T> addSelect(T table) {
        return new ConfigurableBaseQueryImpl.Query1Impl<>(table, this);
    }

    @Override
    public <T extends AbstractTypedEmbeddedPropExpression<?>> ConfigurableBaseQuery.Query1<T> addSelect(T expr) {
        return new ConfigurableBaseQueryImpl.Query1Impl<>(expr, this);
    }

    @Override
    public <V> ConfigurableBaseQuery.Query1<Expression<V>> addSelect(Expression<V> expr) {
        return new ConfigurableBaseQueryImpl.Query1Impl<>(expr, this);
    }

    @Override
    public ConfigurableBaseQuery.Query1<StringExpression> addSelect(StringExpression expr) {
        return new ConfigurableBaseQueryImpl.Query1Impl<>(expr, this);
    }

    @Override
    public <V extends Number & Comparable<V>> ConfigurableBaseQuery.Query1<NumericExpression<V>> addSelect(NumericExpression<V> expr) {
        return new ConfigurableBaseQueryImpl.Query1Impl<>(expr, this);
    }

    @Override
    public <V extends Comparable<?>> ConfigurableBaseQuery.Query1<ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
        return new ConfigurableBaseQueryImpl.Query1Impl<>(expr, this);
    }

    @Override
    public <V extends Date> ConfigurableBaseQuery.Query1<DateExpression<V>> addSelect(DateExpression<V> expr) {
        return new ConfigurableBaseQueryImpl.Query1Impl<>(expr, this);
    }

    @Override
    public <V extends Temporal & Comparable<?>> ConfigurableBaseQuery.Query1<TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
        return new ConfigurableBaseQueryImpl.Query1Impl<>(expr, this);
    }

    @Override
    public String toString() {
        return "MutableBaseQueryImpl{queryType = " + getTable() + "}";
    }
}
