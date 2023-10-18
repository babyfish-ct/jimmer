package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.FilterableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.*;

public abstract class AbstractMutableStatementImpl implements FilterableImplementor {

    private static final Predicate[] EMPTY_PREDICATES = new Predicate[0];

    private final JSqlClientImplementor sqlClient;

    private final ImmutableType type;

    private List<Predicate> predicates = new ArrayList<>();

    private Table<?> table;

    private TableImplementor<?> tableImplementor;

    private boolean frozen;

    private int modCount;

    private final Set<TableImplementor<?>> filteredTables = new HashSet<>();

    public AbstractMutableStatementImpl(
            JSqlClientImplementor sqlClient,
            ImmutableType type
    ) {
        if (!type.isEntity()) {
            throw new IllegalArgumentException("\"" + type + "\" is not entity");
        }
        if (sqlClient != null && !sqlClient.getMicroServiceName().equals(type.getMicroServiceName())) {
            throw new IllegalArgumentException(
                    "The sql client and entity type \"" +
                            type +
                            "\" do not belong to the same micro service: " +
                            "{sqlClient: \"" +
                            sqlClient.getMicroServiceName() +
                            "\", entity: \"" +
                            type.getMicroServiceName() +
                            "\"}"
            );
        }
        this.sqlClient = sqlClient;
        this.type = type;
    }

    public AbstractMutableStatementImpl(
            JSqlClientImplementor sqlClient,
            TableProxy<?> table
    ) {
        if (table.__unwrap() != null) {
            throw new IllegalArgumentException("table proxy cannot be wrapper");
        }
        if (table.__prop() != null) {
            throw new IllegalArgumentException("table proxy must be root table");
        }
        this.sqlClient = Objects.requireNonNull(
                sqlClient,
                "sqlClient cannot be null"
        );
        if (!sqlClient.getMicroServiceName().equals(table.getImmutableType().getMicroServiceName())) {
            throw new IllegalArgumentException(
                    "The sql client and entity type \"" +
                            table.getImmutableType() +
                            "\" do not belong to the same micro service: " +
                            "{sqlClient: \"" +
                            sqlClient.getMicroServiceName() +
                            "\", entity: \"" +
                            table.getImmutableType().getMicroServiceName() +
                            "\"}"
            );
        }
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
        StatementContext ctx = getContext();
        FilterLevel filterLevel = ctx != null ? ctx.getFilterLevel() : FilterLevel.DEFAULT;
        if (filterLevel != FilterLevel.IGNORE_ALL) {
            Filter<Props> globalFilter = getSqlClient().getFilters().getFilter(type);
            if (globalFilter != null) {
                applyGlobalFilers(filterLevel);
            }
        }
        predicates = mergePredicates(predicates);
    }

    protected final void applyGlobalFilers(FilterLevel level) {
        applyGlobalFiler(getTableImplementor(), level);
        int modCount = 0;
        AstVisitor visitor = new ApplyFilterVisitor(level);
        AstContext astContext = visitor.getAstContext();
        astContext.pushStatement(this);
        try {
            int predicateIndex = 0;
            __APPLY_FILTERS__:
            while (modCount() != modCount) {
                modCount = modCount();
                ListIterator<Predicate> itr = predicates.listIterator(predicateIndex);
                while (itr.hasNext()) {
                    ((Ast) itr.next()).accept(visitor);
                    if (modCount() != modCount) {
                        predicateIndex = itr.nextIndex();
                        continue __APPLY_FILTERS__;
                    }
                }
                for (Order order : getOrders()) {
                    ((Ast) order.getExpression()).accept(visitor);
                    if (modCount() != modCount) {
                        continue __APPLY_FILTERS__;
                    }
                }
            }
        } finally {
            astContext.popStatement();
        }
    }

    public final void applyGlobalFiler(TableImplementor<?> table, FilterLevel level) {
        if (level == FilterLevel.IGNORE_ALL || !filteredTables.add(table)) {
            return;
        }
        Filter<Props> globalFilter;
        if (level == FilterLevel.IGNORE_USER_FILTERS) {
            globalFilter = getSqlClient().getFilters().getLogicalDeletedFilter(table.getImmutableType());
        } else {
            globalFilter = getSqlClient().getFilters().getFilter(table.getImmutableType());
        }
        if (globalFilter != null) {
            FilterArgsImpl<Props> args = new FilterArgsImpl<>(
                    this,
                    TableProxies.wrap(table),
                    false
            );
            globalFilter.filter(args);
        }
    }

    protected List<Order> getOrders() {
        return Collections.emptyList();
    }

    public void validateMutable() {
        if (frozen) {
            throw new IllegalStateException(
                    "Cannot mutate the statement because it has been frozen"
            );
        }
    }

    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }

    @Override
    public Filterable where(Predicate ... predicates) {
        validateMutable();
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                this.predicates.add(predicate);
                modCount++;
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
                        predicates.toArray(EMPTY_PREDICATES)
                )
        );
    }

    protected final int modCount() {
        return modCount;
    }

    protected final void modify() {
        modCount++;
    }

    protected class ApplyFilterVisitor extends AstVisitor {

        private final FilterLevel level;

        public ApplyFilterVisitor(FilterLevel level) {
            super(new AstContext(sqlClient));
            this.level = level;
        }

        @Override
        public void visitTableReference(TableImplementor<?> table, ImmutableProp prop, boolean rawId) {
            if (prop.isId() && (rawId || table.isRawIdAllowed(getAstContext().getSqlClient()))) {
                table = table.getParent();
            }
            while (table != null) {
                applyGlobalFiler(table, level);
                table = table.getParent();
            }
        }
    }
}
