package org.babyfish.jimmer.sql.ast.query.specification;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.util.function.Supplier;

public class SpecificationArgs<E, T extends TableLike<E>> {

    private final PredicateApplier applier;

    private final T table;

    private final AbstractMutableStatementImpl query;

    public SpecificationArgs(PredicateApplier applier) {
        this.applier = applier;
        this.table = applier.getQuery().getTable();
        this.query = getApplier().getQuery();
    }

    public T getTable() {
        return table;
    }

    public SpecificationArgs<E, T> where(Predicate ... predicates) {
        query.where(predicates);
        return this;
    }

    public SpecificationArgs<E, T> where(boolean condition, Predicate predicate) {
        query.whereIf(condition, predicate);
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
    @Deprecated
    public SpecificationArgs<E, T> whereIf(boolean condition, Predicate predicate) {
        query.whereIf(condition, predicate);
        return this;
    }

    public SpecificationArgs<E, T> whereIf(boolean condition, Supplier<Predicate> block) {
        query.whereIf(condition, block);
        return this;
    }

    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return query.createSubQuery(table);
    }

    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>> MutableSubQuery createAssociationSubQuery(
            AssociationTable<SE, ST, TE, TT> table
    ) {
        return query.createAssociationSubQuery(table);
    }

    public PredicateApplier getApplier() {
        return applier;
    }

    public <XE, XT extends Table<XE>> SpecificationArgs<XE, XT> child() {
        return new SpecificationArgs<>(applier);
    }
}
