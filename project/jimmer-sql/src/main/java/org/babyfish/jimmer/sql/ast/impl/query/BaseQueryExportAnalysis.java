package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryScope;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionMapper;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;

final class BaseQueryExportAnalysis {

    private BaseQueryExportAnalysis() {}

    static void analyze(AbstractMutableStatementImpl statement, AstContext ctx) {
        BaseQueryScope scope = ctx.getBaseQueryScope();
        if (scope == null) {
            return;
        }
        analyze(statement.getTableLikeImplementor(), ctx);
    }

    private static void analyze(
            TableLikeImplementor<?> tableLikeImplementor,
            AstContext ctx
    ) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            analyze((BaseTableImplementor) tableLikeImplementor, ctx);
        } else {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) tableLikeImplementor;
            if (tableImplementor.hasBaseTable()) {
                Iterable<TableLikeImplementor<?>> children =
                        (Iterable<TableLikeImplementor<?>>) tableImplementor;
                for (TableLikeImplementor<?> child : children) {
                    analyze(child, ctx);
                }
            }
        }
    }

    private static void analyze(
            BaseTableImplementor baseTableImplementor,
            AstContext ctx
    ) {
        for (Selection<?> selection : baseTableImplementor.toSymbol().getSelections()) {
            if (!(selection instanceof Table<?>)) {
                continue;
            }
            Table<?> table = (Table<?>) selection;
            TableImplementor<?> tableImplementor = TableProxies.resolve(table, ctx);
            BaseSelectionMapper mapper = ctx.getBaseSelectionMapper(tableImplementor.getBaseTableOwner());
            if (mapper == null) {
                continue;
            }
            RealTable realTable = tableImplementor.realTable(ctx);
            for (RealTable childTable : realTable) {
                if (!(childTable.getTableLikeImplementor() instanceof TableImplementor<?>)) {
                    continue;
                }
                TableImplementor<?> childTableImplementor =
                        (TableImplementor<?>) childTable.getTableLikeImplementor();
                ImmutableProp prop = childTableImplementor.getJoinProp();
                if (prop == null) {
                    break;
                }
                if (childTableImplementor.isInverse()) {
                    prop = prop.getOpposite();
                    if (prop == null) {
                        continue;
                    }
                }
                if (!prop.isColumnDefinition()) {
                    continue;
                }
                ColumnDefinition definition = prop.getStorage(ctx.getSqlClient().getMetadataStrategy());
                int size = definition.size();
                for (int i = 0; i < size; i++) {
                    mapper.columnIndex(realTable.getAlias(), definition.name(i), false);
                }
            }
        }
    }
}
