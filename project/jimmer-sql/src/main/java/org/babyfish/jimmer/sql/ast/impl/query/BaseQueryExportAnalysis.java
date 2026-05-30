package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportCollectorSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlTemplate;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.Nullable;

final class BaseQueryExportAnalysis {

    private BaseQueryExportAnalysis() {}

    static void analyze(AbstractMutableStatementImpl statement, QueryAnalysisBuilder analysis) {
        if (!TableUtils.hasBaseTable(statement.getTableLikeImplementor())) {
            return;
        }
        analyze(statement.getTableLikeImplementor(), analysis);
    }

    static void analyzeUsages(QueryAnalysisBuilder analysis) {
        for (BaseTableOwner baseTableOwner : analysis.baseQueryExportOwners()) {
            analyze(baseTableOwner, analysis);
        }
    }

    private static void analyze(
            TableLikeImplementor<?> tableLikeImplementor,
            QueryAnalysisBuilder analysis
    ) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            analyze((BaseTableImplementor) tableLikeImplementor, analysis);
        }
        if (!tableLikeImplementor.hasBaseTable() || !(tableLikeImplementor instanceof Iterable<?>)) {
            return;
        }
        for (Object child : (Iterable<?>) tableLikeImplementor) {
            if (child instanceof TableLikeImplementor<?>) {
                analyze((TableLikeImplementor<?>) child, analysis);
            }
        }
    }

    private static void analyze(
            BaseTableImplementor baseTableImplementor,
            QueryAnalysisBuilder analysis
    ) {
        QueryAnalysisContext ctx = analysis.getAnalysisContext();
        java.util.List<Selection<?>> selections = baseTableImplementor.getSelections();
        int size = selections.size();
        for (int index = 0; index < size; index++) {
            Selection<?> selection = selections.get(index);
            if (selection instanceof Table<?>) {
                analyzeTableSelection(baseTableImplementor, (Table<?>) selection, index, analysis, ctx);
            } else {
                analyzeExpressionSelection(baseTableImplementor, index, analysis);
            }
        }
    }

    private static void analyze(
            BaseTableOwner baseTableOwner,
            QueryAnalysisBuilder analysis
    ) {
        QueryAnalysisContext ctx = analysis.getAnalysisContext();
        BaseTableImplementor baseTableImplementor = ctx.resolveBaseTable(baseTableOwner.getBaseTable());
        if (baseTableImplementor == null) {
            Selection<?> selection = baseTableOwner
                    .getBaseTable()
                    .getSelections()
                    .get(baseTableOwner.getIndex());
            if (!(selection instanceof Table<?>)) {
                analyzeExpressionSelection(baseTableOwner, analysis);
            }
            return;
        }
        Selection<?> selection = baseTableImplementor.getSelections().get(baseTableOwner.getIndex());
        if (selection instanceof Table<?>) {
            analyzeTableSelection(baseTableImplementor, (Table<?>) selection, baseTableOwner.getIndex(), analysis, ctx);
        } else {
            analyzeExpressionSelection(baseTableOwner, analysis);
        }
    }

    private static void analyzeTableSelection(
            BaseTableImplementor baseTableImplementor,
            Table<?> table,
            int index,
            QueryAnalysisBuilder analysis,
            QueryAnalysisContext ctx
    ) {
            TableImplementor<?> tableImplementor = ctx.resolve(table);
            BaseTableOwner baseTableOwner = tableImplementor.getBaseTableOwner();
            if (baseTableOwner == null) {
                return;
            }
            BaseQueryExportCollectorSelection exportSelection =
                    analysis.requireBaseQueryExportSelection(baseTableOwner);
            if (exportSelection == null) {
                return;
            }
            RealTable realTable = ctx.realTable(tableImplementor);
            java.util.List<BaseQueryExportUsages.TableReferenceUsage> usages =
                    analysis.tableReferenceUsages(baseTableOwner);
            boolean[] consumedUsages = new boolean[usages.size()];
            boolean fullRow = analysis.isFullRowExportRequired(baseTableOwner);
            for (ImmutableProp prop : tableImplementor.getImmutableType().getSelectableProps().values()) {
                if (fullRow) {
                    analyzeProp(realTable, tableImplementor, prop, false, exportSelection, ctx);
                }
                for (int i = 0; i < usages.size(); i++) {
                    BaseQueryExportUsages.TableReferenceUsage usage = usages.get(i);
                    if (consumedUsages[i] ||
                            usage.prop != prop ||
                            !exportSelection.isRootTable(usage.table)) {
                        continue;
                    }
                    analyzeProp(
                            realTable,
                            tableImplementor,
                            usage.prop,
                            usage.rawId,
                            usage.columnNames,
                            exportSelection,
                            ctx
                    );
                    consumedUsages[i] = true;
                }
            }
            for (int i = 0; i < usages.size(); i++) {
                if (!consumedUsages[i]) {
                    analyzeTableReference(usages.get(i), exportSelection, ctx);
                }
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
                ColumnDefinition definition = prop.getStorage(ctx.getMetadataStrategy());
                int size = definition.size();
                for (int i = 0; i < size; i++) {
                    exportSelection.requireJoinKeyColumnIndex(realTable, definition.name(i), false);
                }
            }
    }

    private static void analyzeExpressionSelection(
            BaseTableImplementor baseTableImplementor,
            int index,
            QueryAnalysisBuilder analysis
    ) {
        analyzeExpressionSelection(new BaseTableOwner(baseTableImplementor, index), analysis);
    }

    private static void analyzeExpressionSelection(
            BaseTableOwner baseTableOwner,
            QueryAnalysisBuilder analysis
    ) {
        if (analysis.isExpressionExportRequired(baseTableOwner)) {
            BaseQueryExportCollectorSelection exportSelection =
                    analysis.requireBaseQueryExportSelection(baseTableOwner);
            if (exportSelection != null) {
                exportSelection.requireExpressionIndex();
            }
        }
    }

    private static void analyzeTableReference(
            BaseQueryExportUsages.TableReferenceUsage usage,
            BaseQueryExportCollectorSelection exportSelection,
            QueryAnalysisContext ctx
    ) {
        RealTable table = usage.table;
        TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
        if (!(implementor instanceof TableImplementor<?>)) {
            return;
        }
        if (usage.prop == null || !exportSelection.containsTable(table)) {
            return;
        }
        if (!exportSelection.isRootTable(table)) {
            if (!isForeignKeyIdOnSelectedRow(
                    table,
                    (TableImplementor<?>) implementor,
                    usage.prop,
                    usage.rawId,
                    exportSelection,
                    ctx
            )) {
                return;
            }
        }
        analyzeProp(
                table,
                (TableImplementor<?>) implementor,
                usage.prop,
                usage.rawId,
                usage.columnNames,
                exportSelection,
                ctx
        );
    }

    private static boolean isForeignKeyIdOnSelectedRow(
            RealTable table,
            TableImplementor<?> tableImplementor,
            ImmutableProp prop,
            boolean rawId,
            BaseQueryExportCollectorSelection exportSelection,
            QueryAnalysisContext ctx
    ) {
        ImmutableProp joinProp = tableImplementor.getJoinProp();
        return prop.isId() &&
                joinProp != null &&
                !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(tableImplementor, ctx.getSqlClient())) &&
                !tableImplementor.isInverse() &&
                !joinProp.isMiddleTableDefinition() &&
                table.getParent() != null &&
                exportSelection.containsTable(table.getParent());
    }

    private static void analyzeProp(
            RealTable table,
            TableImplementor<?> tableImplementor,
            ImmutableProp prop,
            boolean rawId,
            BaseQueryExportCollectorSelection exportSelection,
            QueryAnalysisContext ctx
    ) {
        analyzeProp(table, tableImplementor, prop, rawId, null, exportSelection, ctx);
    }

    private static void analyzeProp(
            RealTable table,
            TableImplementor<?> tableImplementor,
            ImmutableProp prop,
            boolean rawId,
            @Nullable java.util.List<String> columnNames,
            BaseQueryExportCollectorSelection exportSelection,
            QueryAnalysisContext ctx
    ) {
        SqlTemplate template = prop.getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            exportSelection.requireFormulaIndex(table, (FormulaTemplate) template);
            return;
        }
        Storage storage = prop.getStorage(ctx.getMetadataStrategy());
        if (storage instanceof EmbeddedColumns && columnNames != null) {
            analyzeColumns(table, columnNames, false, exportSelection);
            return;
        }
        if (!(storage instanceof ColumnDefinition)) {
            return;
        }
        MetadataStrategy strategy = ctx.getMetadataStrategy();
        ImmutableProp joinProp = tableImplementor.getJoinProp();
        if (prop.isId() &&
                joinProp != null &&
                !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(tableImplementor, ctx.getSqlClient())) &&
                !tableImplementor.isInverse() &&
                !joinProp.isMiddleTableDefinition() &&
                table.getParent() != null) {
            ColumnDefinition definition = joinProp.getStorage(strategy);
            analyzeColumns(table.getParent(), definition, true, exportSelection);
            return;
        }
        ColumnDefinition definition = (ColumnDefinition) storage;
        analyzeColumns(table, definition, false, exportSelection);
    }

    private static void analyzeColumns(
            RealTable table,
            ColumnDefinition definition,
            boolean foreignKeyInBaseQuery,
            BaseQueryExportCollectorSelection exportSelection
    ) {
        int size = definition.size();
        for (int i = 0; i < size; i++) {
            exportSelection.requireColumnIndex(table, definition.name(i), foreignKeyInBaseQuery);
        }
    }

    private static void analyzeColumns(
            RealTable table,
            java.util.List<String> columnNames,
            boolean foreignKeyInBaseQuery,
            BaseQueryExportCollectorSelection exportSelection
    ) {
        for (String columnName : columnNames) {
            exportSelection.requireColumnIndex(table, columnName, foreignKeyInBaseQuery);
        }
    }
}
