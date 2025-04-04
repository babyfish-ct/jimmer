package org.babyfish.jimmer.sql.ast.query.specification;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.SubQueryProvider;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.util.function.Supplier;

public class SpecificationArgs<E, T extends Table<E>> {

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
