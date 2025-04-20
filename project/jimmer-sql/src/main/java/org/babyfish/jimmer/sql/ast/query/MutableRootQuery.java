package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.sql.Connection;
import java.util.List;
import java.util.function.Supplier;

public interface MutableRootQuery<T extends TableLike<?>> extends MutableQuery, RootSelectable<T> {

    @OldChain
    @Override
    MutableRootQuery<T> where(Predicate... predicates);

    @OldChain
    MutableRootQuery<T> where(Specification<?> specification);

    @OldChain
    MutableRootQuery<T> where(JSpecification<?, T> specification);

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
    default MutableRootQuery<T> whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    @OldChain
    @Override
    default MutableRootQuery<T> whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

    @OldChain
    @Override
    MutableRootQuery<T> orderBy(Expression<?>... expressions);

    @OldChain
    @Override
    default MutableRootQuery<T> orderByIf(boolean condition, Expression<?>... expressions) {
        if (condition) {
            orderBy(expressions);
        }
        return this;
    }

    @OldChain
    @Override
    MutableRootQuery<T> orderBy(Order... orders);

    @OldChain
    @SuppressWarnings("unchecked")
    @Override
    default MutableRootQuery<T> orderByIf(boolean condition, Order... orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }

    @OldChain
    @Override
    MutableRootQuery<T> orderBy(List<Order> orders);

    @OldChain
    @Override
    default MutableRootQuery<T> orderByIf(boolean condition, List<Order> orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }

    @Override
    MutableRootQuery<T> groupBy(Expression<?>... expressions);

    @Override
    MutableRootQuery<T> having(Predicate... predicates);

    default boolean exists() {
        return exists(null);
    }

    boolean exists(Connection con);
}
