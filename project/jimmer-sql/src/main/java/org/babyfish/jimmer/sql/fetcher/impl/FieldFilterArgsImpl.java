package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.fetcher.spi.FieldFilterArgsImplementor;

import java.util.*;
import java.util.function.Supplier;

public class FieldFilterArgsImpl<T extends Table<?>> implements FieldFilterArgsImplementor<T> {

    private final AbstractMutableQueryImpl query;

    private final T table;

    private final Collection<Object> keys;

    private boolean sorted;

    public static <T extends Table<?>> FieldFilterArgsImpl<T> of(
            AbstractMutableQueryImpl query,
            T table,
            Collection<Object> keys
    ) {
        return new FieldFilterArgsImpl<>(
                query,
                table,
                Objects.requireNonNull(keys, "keys cannot be null")
        );
    }

    private FieldFilterArgsImpl(
            AbstractMutableQueryImpl query,
            T table,
            Collection<Object> keys
    ) {
        this.query = query;
        this.table = table;
        this.keys = keys != null ? Collections.unmodifiableCollection(keys) : null;
    }

    public boolean isSorted() {
        return sorted;
    }

    @Override
    public T getTable() {
        return table;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> K getKey() {
        if (keys.size() > 1) {
            throw new IllegalStateException(
                    "Too much keys"
            );
        }
        return (K) keys.iterator().next();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> Collection<K> getKeys() {
        return (Collection<K>)keys;
    }

    @Override
    @OldChain
    public Sortable where(Predicate... predicates) {
        return query.where(predicates);
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
    @Override
    @OldChain
    @Deprecated
    public Sortable whereIf(boolean condition, Predicate predicate) {
        return query.whereIf(condition, predicate);
    }

    @Override
    @OldChain
    public Sortable whereIf(boolean condition, Supplier<Predicate> block) {
        return query.whereIf(condition, block);
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?> ... expressions) {
        if (!sorted) {
            sorted = Arrays.stream(expressions).anyMatch(Objects::nonNull);
        }
        return query.orderBy(expressions);
    }

    @Override
    @OldChain
    public Sortable orderBy(Order ... orders) {
        if (!sorted) {
            sorted = Arrays.stream(orders).anyMatch(Objects::nonNull);
        }
        return query.orderBy(orders);
    }

    @Override
    @OldChain
    public Sortable orderBy(List<Order> orders) {
        if (!sorted) {
            sorted = orders.stream().anyMatch(Objects::nonNull);
        }
        return query.orderBy(orders);
    }

    @Override
    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return query.createSubQuery(table);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>>
    MutableSubQuery createAssociationSubQuery(
            AssociationTable<SE, ST, TE, TT> table
    ) {
        return query.createAssociationSubQuery(table);
    }

    @Override
    public AbstractMutableQueryImpl query() {
        return query;
    }
}
