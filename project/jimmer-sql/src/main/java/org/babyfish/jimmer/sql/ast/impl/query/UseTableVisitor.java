package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
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
        if (prop == null) {
            if (table.getTableImplementor().getImmutableType().getSelectableProps().size() > 1) {
                use(table);
            }
        } else if (prop.isId() && (
                rawId || TableUtils.isRawIdAllowed(table.getTableImplementor(), getAstContext().getSqlClient()))
        ) {
            getAstContext().useTableId(table);
            use(table.getParent());
        } else {
            use(table);
        }
    }

    @Override
    public void visitTableFetcher(RealTable table, Fetcher<?> fetcher) {
        new UseJoinFetcherVisitor(getAstContext(), table.getTableImplementor()).visit(fetcher);
    }

    @Override
    public void visitStatement(AbstractMutableStatementImpl statement) {
        AstContext ctx = getAstContext();
        RealTable table = ctx.getStatement().getTableImplementor().realTable(ctx.getJoinTypeMergeScope());
        rootTables.add(table);
        table.use(this);
    }

    @Override
    public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
        return true;
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
            TableImplementor<?> newTableImplementor = oldTableImplementor.joinFetchImplementor(field.getProp());
            ctx.useTable(newTableImplementor.realTable(ctx.getJoinTypeMergeScope()));
            this.tableImplementor = newTableImplementor;
            return oldTableImplementor;
        }

        @Override
        protected void leave(Field field, Object enterValue) {
            this.tableImplementor = (TableImplementor<?>) enterValue;
        }
    }
}
