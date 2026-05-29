package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BaseQueryExport {

    private final BaseQueryScope scope;

    private final RealTable realBaseTable;

    private final Map<Integer, SelectionExport> selectionExportMap =
            new LinkedHashMap<>();

    private final Map<Integer, BaseQueryExportSelection> selectionMap =
            new LinkedHashMap<>();

    BaseQueryExport(BaseQueryScope scope, RealTable realBaseTable) {
        this.scope = scope;
        this.realBaseTable = realBaseTable;
    }

    public RealTable getRealBaseTable() {
        return realBaseTable;
    }

    public BaseQueryExportSelection selection(int selectionIndex) {
        return selectionMap.computeIfAbsent(
                selectionIndex,
                it -> {
                    Selection<?> selection = ((BaseTableImplementor) realBaseTable.getTableLikeImplementor())
                            .getSelections()
                            .get(it);
                    return new BaseQueryExportSelection(this, it, rootRealTable(selection));
                }
        );
    }

    BaseQueryExportCollectorSelection requireSelection(int selectionIndex) {
        BaseQueryExportSelection selection = selection(selectionIndex);
        if (selection instanceof BaseQueryExportCollectorSelection) {
            return (BaseQueryExportCollectorSelection) selection;
        }
        Selection<?> rawSelection = ((BaseTableImplementor) realBaseTable.getTableLikeImplementor())
                .getSelections()
                .get(selectionIndex);
        BaseQueryExportCollectorSelection collectorSelection =
                new BaseQueryExportCollectorSelection(this, selectionIndex, rootRealTable(rawSelection));
        selectionMap.put(selectionIndex, collectorSelection);
        return collectorSelection;
    }

    public BaseQueryExportSelection selectionOrNull(int selectionIndex) {
        if (!selectionExportMap.containsKey(selectionIndex)) {
            return null;
        }
        return selection(selectionIndex);
    }

    BaseQueryExportColumn column(
            BaseQueryExportSelection selection,
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery
    ) {
        return selectionExportMap.get(selection.getIndex()).columnOrNull(
                tableKeys,
                name,
                foreignKeyInBaseQuery
        );
    }

    BaseQueryExportColumn requireColumn(
            BaseQueryExportSelection selection,
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery
    ) {
        return requireSelectionExport(selection.getIndex()).column(
                tableKeys,
                name,
                foreignKeyInBaseQuery,
                BaseQueryExportColumnRole.SELECTION
        );
    }

    Integer columnIndexOrNull(
            BaseQueryExportSelection selection,
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery
    ) {
        SelectionExport export = selectionExportMap.get(selection.getIndex());
        if (export == null) {
            return null;
        }
        BaseQueryExportColumn column = export.columnOrNull(tableKeys, name, foreignKeyInBaseQuery);
        return column != null ? column.getIndex() : null;
    }

    BaseQueryExportColumn requireJoinKeyColumn(
            BaseQueryExportSelection selection,
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery
    ) {
        return requireSelectionExport(selection.getIndex()).column(
                tableKeys,
                name,
                foreignKeyInBaseQuery,
                BaseQueryExportColumnRole.JOIN_KEY
        );
    }

    BaseQueryExportColumn formula(
            BaseQueryExportSelection selection,
            List<RealTable.Key> tableKeys,
            FormulaTemplate formula
    ) {
        return selectionExportMap.get(selection.getIndex()).formulaOrNull(tableKeys, formula);
    }

    BaseQueryExportColumn requireFormula(
            BaseQueryExportSelection selection,
            List<RealTable.Key> tableKeys,
            FormulaTemplate formula
    ) {
        return requireSelectionExport(selection.getIndex()).formula(tableKeys, formula);
    }

    Collection<BaseQueryExportColumn> columns(int selectionIndex) {
        SelectionExport export = selectionExportMap.get(selectionIndex);
        return export != null ? export.columns() : java.util.Collections.emptyList();
    }

    int expressionIndex(BaseQueryExportSelection selection) {
        return selectionExportMap.get(selection.getIndex()).expressionIndex();
    }

    int requireExpressionIndex(BaseQueryExportSelection selection) {
        return requireSelectionExport(selection.getIndex()).requireExpressionIndex();
    }

    AstContext astContext() {
        return scope.astContext;
    }

    int nextColumnIndex() {
        return scope.colNo();
    }

    private RealTable rootRealTable(Selection<?> selection) {
        if (!(selection instanceof Table<?>)) {
            return null;
        }
        return TableProxies
                .resolve((Table<?>) selection, astContext())
                .realTable(astContext());
    }

    private SelectionExport requireSelectionExport(int selectionIndex) {
        SelectionExport export = selectionExportMap.get(selectionIndex);
        if (export == null) {
            export = new SelectionExport();
            selectionExportMap.put(selectionIndex, export);
        }
        return export;
    }

    private final class SelectionExport {

        private final Map<BaseQueryExportColumn.Key, BaseQueryExportColumn> columnMap =
                new LinkedHashMap<>();

        private Integer expressionIndex;

        private SelectionExport() {
        }

        BaseQueryExportColumn column(
                List<RealTable.Key> tableKeys,
                String name,
                boolean foreignKeyInBaseQuery,
                BaseQueryExportColumnRole role
        ) {
            BaseQueryExportColumn.Key key =
                    new BaseQueryExportColumn.Key(tableKeys, name, null, foreignKeyInBaseQuery);
            BaseQueryExportColumn column = columnMap.get(key);
            if (column == null) {
                column = compatibleColumn(tableKeys, name);
            }
            if (column == null) {
                column = new BaseQueryExportColumn(
                        tableKeys,
                        name,
                        foreignKeyInBaseQuery,
                        role,
                        nextColumnIndex()
                );
                columnMap.put(key, column);
            }
            return column;
        }

        BaseQueryExportColumn columnOrNull(
                List<RealTable.Key> tableKeys,
                String name,
                boolean foreignKeyInBaseQuery
        ) {
            BaseQueryExportColumn.Key key =
                    new BaseQueryExportColumn.Key(tableKeys, name, null, foreignKeyInBaseQuery);
            BaseQueryExportColumn column = columnMap.get(key);
            return column != null ? column : compatibleColumn(tableKeys, name);
        }

        BaseQueryExportColumn formula(
                List<RealTable.Key> tableKeys,
                FormulaTemplate formula
        ) {
            BaseQueryExportColumn.Key key =
                    new BaseQueryExportColumn.Key(tableKeys, null, formula, false);
            BaseQueryExportColumn column = columnMap.get(key);
            if (column == null) {
                column = new BaseQueryExportColumn(
                        tableKeys,
                        formula,
                        nextColumnIndex()
                );
                columnMap.put(key, column);
            }
            return column;
        }

        BaseQueryExportColumn formulaOrNull(
                List<RealTable.Key> tableKeys,
                FormulaTemplate formula
        ) {
            BaseQueryExportColumn.Key key =
                    new BaseQueryExportColumn.Key(tableKeys, null, formula, false);
            return columnMap.get(key);
        }

        int expressionIndex() {
            return expressionIndex;
        }

        int requireExpressionIndex() {
            Integer index = expressionIndex;
            if (index == null) {
                expressionIndex = index = nextColumnIndex();
            }
            return index;
        }

        Collection<BaseQueryExportColumn> columns() {
            return columnMap.values();
        }

        private BaseQueryExportColumn compatibleColumn(List<RealTable.Key> tableKeys, String name) {
            for (BaseQueryExportColumn column : columnMap.values()) {
                if (column.getFormula() == null &&
                        column.getTableKeys().equals(tableKeys) &&
                        name.equals(column.getName())) {
                    return column;
                }
            }
            return null;
        }
    }
}
