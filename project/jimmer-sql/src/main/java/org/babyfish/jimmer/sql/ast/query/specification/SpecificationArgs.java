package org.babyfish.jimmer.sql.ast.query.specification;

import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.SubQueryProvider;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

public class SpecificationArgs<E, T extends Table<E>> {

    private final PredicateApplier applier;

    private final T table;

    private final SubQueryProvider provider;

    public SpecificationArgs(PredicateApplier applier) {
        this.applier = applier;
        this.table = applier.getQuery().getTable();
        this.provider = getApplier().getQuery();
    }

    public T getTable() {
        return table;
    }

    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return provider.createSubQuery(table);
    }

    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>> MutableSubQuery createAssociationSubQuery(
            AssociationTable<SE, ST, TE, TT> table
    ) {
        return provider.createAssociationSubQuery(table);
    }

    public PredicateApplier getApplier() {
        return applier;
    }

    public <XE, XT extends Table<XE>> SpecificationArgs<XE, XT> child() {
        return new SpecificationArgs<>(applier);
    }
}
