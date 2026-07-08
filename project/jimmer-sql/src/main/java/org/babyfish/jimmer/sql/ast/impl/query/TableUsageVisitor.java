package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.fetcher.impl.JoinFetchFieldVisitor;
import org.jetbrains.annotations.Nullable;

public abstract class TableUsageVisitor extends AstVisitor {

    public TableUsageVisitor(AstContext ctx) {
        super(ctx);
    }

    public TableUsageVisitor(AstContext ctx, QueryAnalysis queryAnalysis) {
        super(ctx, queryAnalysis);
    }

    @Override
    public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
        TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
        if (implementor instanceof BaseTableImplementor) {
            BaseTableImplementor baseTableImplementor = (BaseTableImplementor) implementor;
            baseTableImplementor.realTable(getAstContext()).use(this);
        } else if (implementor instanceof TableImplementor<?>) {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
            if (prop == null) {
                if (tableImplementor.getImmutableType().getSelectableProps().size() > 1) {
                    use(table);
                }
            } else if (prop.isId() && (
                    rawId || TableUtils.isRawIdAllowed(tableImplementor, getAstContext().getSqlClient()))
            ) {
                useTableId(table);
                useColumnSource(table.getParent());
            } else {
                use(table);
            }
            BaseTableOwner owner = tableImplementor.getBaseTableOwner();
            if (owner != null) {
                visitBaseTableReference(owner, table, prop, rawId);
                BaseTableImplementor baseTableImplementor = getAstContext().resolveBaseTable(owner.getBaseTable());
                use(realTable(baseTableImplementor));
            }
        }
    }

    @Override
    public void visitTableFetcher(RealTable table, Fetcher<?> fetcher) {
        TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
        if (implementor instanceof TableImplementor<?>) {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
            new UseJoinFetcherVisitor(getAstContext(), tableImplementor).visit(fetcher);
        }
    }

    @Override
    public void visitStatement(AbstractMutableStatementImpl statement) {
        AstContext ctx = getAstContext();
        RealTable table = realTable(ctx.getStatement().getTableLikeImplementor());
        addRootTable(table);
        table.use(this);
    }

    protected abstract void addRootTable(RealTable table);

    protected abstract void useTableId(RealTable table);

    protected abstract void useTable(RealTable table);

    protected void visitBaseTableReference(
            BaseTableOwner owner,
            RealTable table,
            @Nullable ImmutableProp prop,
            boolean rawId
    ) {}

    protected final RealTable realTable(TableLikeImplementor<?> tableLikeImplementor) {
        return realTableForAnalysis(tableLikeImplementor);
    }

    protected final void use(RealTable table) {
        if (table != null) {
            useTable(table);
            useParent(table);
        }
    }

    private void useParent(RealTable table) {
        RealTable parent = table.getParent();
        if (parent == null) {
            return;
        }
        if (parent.isOptimizableBridgeTo(table, getAstContext())) {
            useTableId(parent);
            use(parent.getParent());
        } else {
            use(parent);
        }
    }

    private void useColumnSource(RealTable table) {
        if (table == null) {
            return;
        }
        if (table.isMappedIdColumnSource(getAstContext())) {
            useTableId(table);
            useColumnSource(table.getParent());
        } else {
            use(table);
        }
    }

    private class UseJoinFetcherVisitor extends JoinFetchFieldVisitor {

        private final AstContext ctx;

        private TableImplementor<?> tableImplementor;

        UseJoinFetcherVisitor(AstContext ctx, TableImplementor<?> tableImplementor) {
            super(ctx.getSqlClient());
            this.ctx = ctx;
            this.tableImplementor = tableImplementor;
        }

        @Override
        protected Object enter(Field field) {
            TableImplementor<?> oldTableImplementor = this.tableImplementor;
            TableImplementor<?> newTableImplementor =
                    oldTableImplementor.joinFetchImplementor(field.getProp(), oldTableImplementor.getBaseTableOwner());
            RealTable realTable = newTableImplementor.realTable(ctx);
            if (isIdOnlyJoinFetch(field)) {
                useTableId(realTable);
            } else {
                useTable(realTable);
            }
            this.tableImplementor = newTableImplementor;
            return oldTableImplementor;
        }

        @Override
        protected void leave(Field field, Object enterValue) {
            this.tableImplementor = (TableImplementor<?>) enterValue;
        }

        @Override
        protected boolean shouldVisitTypeBranch(ImmutableType branchType, Fetcher<?> fetcher) {
            return JoinFetchFieldVisitor.hasTableFields(fetcher, ctx.getSqlClient(), true);
        }

        @Override
        protected Object enterTypeBranch(ImmutableType branchType) {
            TableImplementor<?> oldTableImplementor = this.tableImplementor;
            TableImplementor<?> newTableImplementor =
                    oldTableImplementor.treatAsImplementor(branchType, JoinType.LEFT);
            useTable(newTableImplementor.realTable(ctx));
            this.tableImplementor = newTableImplementor;
            return oldTableImplementor;
        }

        @Override
        protected void leaveTypeBranch(ImmutableType branchType, Object enterValue) {
            this.tableImplementor = (TableImplementor<?>) enterValue;
        }

        private boolean isIdOnlyJoinFetch(Field field) {
            if (field.getFilter() != null || field.getRecursionStrategy() != null) {
                return false;
            }
            if (ctx.getSqlClient().getFilters().getTargetFilter(field.getProp()) != null) {
                return false;
            }
            Fetcher<?> childFetcher = field.getChildFetcher();
            return childFetcher != null && isIdOnlyFetcher(childFetcher);
        }

        private boolean isIdOnlyFetcher(Fetcher<?> fetcher) {
            if (fetcher instanceof FetcherImplementor<?> &&
                    !((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty()) {
                return false;
            }
            for (Field field : fetcher.getFieldMap().values()) {
                ImmutableProp prop = field.getProp();
                if (prop.isId()) {
                    continue;
                }
                if (!prop.isMappedId() ||
                        !prop.isAssociation(TargetLevel.PERSISTENT) ||
                        prop.isReferenceList(TargetLevel.PERSISTENT) ||
                        !isIdOnlyJoinFetch(field)) {
                    return false;
                }
            }
            return true;
        }
    }
}
