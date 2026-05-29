package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlTemplate;
import org.jetbrains.annotations.Nullable;

final class BaseQueryExportAnalysis {

    private BaseQueryExportAnalysis() {}

    static void analyze(AbstractMutableStatementImpl statement, QueryAnalysis analysis) {
        AstContext ctx = analysis.getAstContext();
        if (ctx.findCurrentStatementUsingBaseQuery() == null) {
            return;
        }
        analyze(statement.getTableLikeImplementor(), analysis);
    }

    static void analyzeTableReference(
            RealTable table,
            @Nullable ImmutableProp prop,
            boolean rawId,
            QueryAnalysis analysis
    ) {
        AstContext ctx = analysis.getAstContext();
        BaseQueryExportSelection exportSelection = analysis.getBaseQueryExportSelection(table.getBaseTableOwner());
        if (exportSelection == null) {
            return;
        }
        TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
        if (!(implementor instanceof TableImplementor<?>)) {
            return;
        }
        if (!exportSelection.isRootTable(table)) {
            return;
        }
        TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
        if (prop == null) {
            for (ImmutableProp selectableProp : tableImplementor.getImmutableType().getSelectableProps().values()) {
                analyzeProp(table, tableImplementor, selectableProp, false, exportSelection, ctx);
            }
        } else {
            analyzeProp(table, tableImplementor, prop, rawId, exportSelection, ctx);
        }
    }

    private static void analyze(
            TableLikeImplementor<?> tableLikeImplementor,
            QueryAnalysis analysis
    ) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            analyze((BaseTableImplementor) tableLikeImplementor, analysis);
        } else {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) tableLikeImplementor;
            if (tableImplementor.hasBaseTable()) {
                Iterable<TableLikeImplementor<?>> children =
                        (Iterable<TableLikeImplementor<?>>) tableImplementor;
                for (TableLikeImplementor<?> child : children) {
                    analyze(child, analysis);
                }
            }
        }
    }

    private static void analyze(
            BaseTableImplementor baseTableImplementor,
            QueryAnalysis analysis
    ) {
        AstContext ctx = analysis.getAstContext();
        for (Selection<?> selection : baseTableImplementor.toSymbol().getSelections()) {
            if (!(selection instanceof Table<?>)) {
                continue;
            }
            Table<?> table = (Table<?>) selection;
            TableImplementor<?> tableImplementor = TableProxies.resolve(table, ctx);
            BaseQueryExportSelection exportSelection = analysis.getBaseQueryExportSelection(tableImplementor.getBaseTableOwner());
            if (exportSelection == null) {
                continue;
            }
            RealTable realTable = tableImplementor.realTable(ctx);
            for (ImmutableProp prop : tableImplementor.getImmutableType().getSelectableProps().values()) {
                analyzeProp(realTable, tableImplementor, prop, false, exportSelection, ctx);
            }
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
                    exportSelection.joinKeyColumnIndex(realTable.getAlias(), definition.name(i), false);
                }
            }
        }
    }

    private static void analyzeProp(
            RealTable table,
            TableImplementor<?> tableImplementor,
            ImmutableProp prop,
            boolean rawId,
            BaseQueryExportSelection exportSelection,
            AstContext ctx
    ) {
        SqlTemplate template = prop.getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            exportSelection.formulaIndex(table.getAlias(), (FormulaTemplate) template);
            return;
        }
        if (!prop.isColumnDefinition()) {
            return;
        }
        MetadataStrategy strategy = ctx.getSqlClient().getMetadataStrategy();
        ImmutableProp joinProp = tableImplementor.getJoinProp();
        if (prop.isId() &&
                joinProp != null &&
                !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(tableImplementor, ctx.getSqlClient())) &&
                !tableImplementor.isInverse() &&
                !joinProp.isMiddleTableDefinition() &&
                table.getParent() != null) {
            ColumnDefinition definition = joinProp.getStorage(strategy);
            analyzeColumns(table.getParent().getAlias(), definition, true, exportSelection);
            return;
        }
        ColumnDefinition definition = prop.getStorage(strategy);
        String alias = table.getFinalAlias(prop, rawId, ctx.getSqlClient());
        analyzeColumns(alias, definition, false, exportSelection);
    }

    private static void analyzeColumns(
            String alias,
            ColumnDefinition definition,
            boolean foreignKeyInBaseQuery,
            BaseQueryExportSelection exportSelection
    ) {
        int size = definition.size();
        for (int i = 0; i < size; i++) {
            exportSelection.columnIndex(alias, definition.name(i), foreignKeyInBaseQuery);
        }
    }
}
