package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.FilterableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

import java.util.*;

public abstract class AbstractMutableStatementImpl implements FilterableImplementor {

    private final JSqlClient sqlClient;

    private final ImmutableType type;

    private List<Predicate> predicates = new ArrayList<>();

    private Table<?> table;

    private TableImplementor<?> tableImplementor;

    private boolean frozen;

    public AbstractMutableStatementImpl(
            JSqlClient sqlClient,
            ImmutableType type
    ) {
        if (!type.isEntity()) {
            throw new IllegalArgumentException("\"" + type + "\" is not entity");
        }
        this.sqlClient = sqlClient;
        this.type = type;
    }

    public AbstractMutableStatementImpl(
            JSqlClient sqlClient,
            TableProxy<?> table
    ) {
        if (table.__unwrap() != null) {
            throw new IllegalArgumentException("table proxy cannot be wrapper");
        }
        this.sqlClient = Objects.requireNonNull(
                sqlClient,
                "sqlClient cannot be null"
        );
        this.table = table;
        this.type = table.getImmutableType();
    }

    @SuppressWarnings("unchecked")
    public <T extends Table<?>> T getTable() {
        Table<?> table = this.table;
        if (table == null) {
            this.table = table = TableProxies.wrap(getTableImplementor());
        }
        return (T)table;
    }

    public TableImplementor<?> getTableImplementor() {
        TableImplementor<?> tableImplementor = this.tableImplementor;
        if (tableImplementor == null) {
            this.tableImplementor = tableImplementor =
                    TableImplementor.create(this, type);
        }
        return tableImplementor;
    }

    public Predicate getPredicate() {
        return predicates.isEmpty() ? null : predicates.get(0);
    }

    public abstract StatementContext getContext();

    public abstract AbstractMutableStatementImpl getParent();

    @Override
    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return sqlClient.createSubQuery(table);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>> MutableSubQuery createAssociationSubQuery(
            AssociationTable<SE, ST, TE, TT> table
    ) {
        return sqlClient.createAssociationSubQuery(table);
    }

    public final boolean freeze() {
        if (frozen) {
            return false;
        }
        onFrozen();
        frozen = true;
        return true;
    }

    protected void onFrozen() {
        predicates = mergePredicates(predicates);
    }

    public void validateMutable() {
        if (frozen) {
            throw new IllegalStateException(
                    "Cannot mutate the statement because it has been frozen"
            );
        }
    }

    public JSqlClient getSqlClient() {
        return sqlClient;
    }

    @Override
    public Filterable where(Predicate ... predicates) {
        validateMutable();
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                this.predicates.add(predicate);
            }
        }
        return this;
    }

    public ExecutionPurpose getPurpose() {
        return getContext().getPurpose();
    }

    protected static List<Predicate> mergePredicates(List<Predicate> predicates) {
        if (predicates.size() < 2) {
            return predicates;
        }
        return Collections.singletonList(
                Predicate.and(
                        predicates.toArray(EMPTY_PREDICATE)
                )
        );
    }

    private static final Predicate[] EMPTY_PREDICATE = new Predicate[0];
}
