package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;
import org.babyfish.jimmer.sql.ast.table.spi.TableExProxy;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

public interface MutableBaseQuery extends MutableQuery {

    @OldChain
    @Override
    MutableBaseQuery where(Predicate... predicates);

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
    default MutableBaseQuery whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    @OldChain
    @Override
    default MutableBaseQuery whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

    @OldChain
    @Override
    MutableBaseQuery orderBy(Expression<?>... expressions);

    @OldChain
    @Override
    default MutableBaseQuery orderByIf(boolean condition, Expression<?>... expressions) {
        if (condition) {
            orderBy(expressions);
        }
        return this;
    }

    @OldChain
    @Override
    MutableBaseQuery orderBy(Order... orders);

    @OldChain
    @SuppressWarnings("unchecked")
    @Override
    default MutableBaseQuery orderByIf(boolean condition, Order... orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }

    @OldChain
    @Override
    MutableBaseQuery orderBy(List<Order> orders);

    @OldChain
    @Override
    default MutableBaseQuery orderByIf(boolean condition, List<Order> orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }

    @Override
    MutableBaseQuery groupBy(Expression<?>... expressions);

    @Override
    MutableBaseQuery having(Predicate... predicates);

    <T extends Table<?>> ConfigurableBaseQuery.Query1<T> addSelect(T table);

    @SuppressWarnings("unchecked")
    default <T extends Table<?>, TEX extends TableExProxy<?, T>> ConfigurableBaseQuery.Query1<T> addSelect(TEX table) {
        return addSelect((T)table);
    }

    <T extends AbstractTypedEmbeddedPropExpression<?>> ConfigurableBaseQuery.Query1<T> addSelect(T expr);

    <V> ConfigurableBaseQuery.Query1<Expression<V>> addSelect(Expression<V> expr);

    ConfigurableBaseQuery.Query1<StringExpression> addSelect(StringExpression expr);

    <V extends Number & Comparable<V>> ConfigurableBaseQuery.Query1<NumericExpression<V>> addSelect(NumericExpression<V> expr);

    <V extends Comparable<?>> ConfigurableBaseQuery.Query1<ComparableExpression<V>> addSelect(ComparableExpression<V> expr);

    <V extends Date> ConfigurableBaseQuery.Query1<DateExpression<V>> addSelect(DateExpression<V> expr);

    <V extends Temporal & Comparable<?>> ConfigurableBaseQuery.Query1<TemporalExpression<V>> addSelect(TemporalExpression<V> expr);
}
