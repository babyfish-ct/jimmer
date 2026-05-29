package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
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
                use(table.getParent());
            } else {
                use(table);
            }
            BaseTableOwner owner = tableImplementor.getBaseTableOwner();
            if (owner != null) {
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

    protected final RealTable realTable(TableLikeImplementor<?> tableLikeImplementor) {
        return getQueryRenderContext() != null ?
                tableLikeImplementor.realTable(getQueryRenderContext()) :
                tableLikeImplementor.realTable(getAstContext());
    }

    protected final void use(RealTable table) {
        if (table != null) {
            useTable(table);
            use(table.getParent());
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
            useTable(newTableImplementor.realTable(ctx));
            this.tableImplementor = newTableImplementor;
            return oldTableImplementor;
        }

        @Override
        protected void leave(Field field, Object enterValue) {
            this.tableImplementor = (TableImplementor<?>) enterValue;
        }
    }
}
