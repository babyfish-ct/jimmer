package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.JoinFetchFieldVisitor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UseTableVisitor extends AstVisitor {

    private final List<RealTable> rootTables = new ArrayList<>();

    public UseTableVisitor(AstContext ctx) {
        super(ctx);
    }

    public void allocateAliases() {
        for (RealTable rootTable : rootTables) {
            rootTable.allocateAliases();
        }
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
                getAstContext().useTableId(table);
                use(table.getParent());
            } else {
                use(table);
            }
            BaseTableOwner owner = tableImplementor.getBaseTableOwner();
            if (owner != null) {
                BaseTableImplementor baseTableImplementor = getAstContext().resolveBaseTable(owner.getBaseTable());
                use(baseTableImplementor.realTable(getAstContext()));
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
        RealTable table = ctx.getStatement().getTableLikeImplementor().realTable(ctx);
        rootTables.add(table);
        table.use(this);
    }

    private void use(RealTable table) {
        if (table != null) {
            getAstContext().useTable(table);
            use(table.getParent());
        }
    }

    private static class UseJoinFetcherVisitor extends JoinFetchFieldVisitor {

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
            ctx.useTable(newTableImplementor.realTable(ctx));
            this.tableImplementor = newTableImplementor;
            return oldTableImplementor;
        }

        @Override
        protected void leave(Field field, Object enterValue) {
            this.tableImplementor = (TableImplementor<?>) enterValue;
        }
    }
}
