package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BaseQueryExport {

    private final BaseQueryScope scope;

    private final RealTable realBaseTable;

    private final Map<Integer, SelectionExport> selectionExportMap = new LinkedHashMap<>();

    private final Map<Integer, BaseQueryExportSelection> selectionMap = new LinkedHashMap<>();

    BaseQueryExport(BaseQueryScope scope, RealTable realBaseTable) {
        this.scope = scope;
        this.realBaseTable = realBaseTable;
    }

    public RealTable getRealBaseTable() {
        return realBaseTable;
    }

    public BaseQueryExportSelection selection(int selectionIndex, RealTable rootRealTable) {
        return selectionMap.computeIfAbsent(
                selectionIndex,
                it -> new BaseQueryExportSelection(this, it, rootRealTable)
        );
    }

    BaseQueryExportCollectorSelection requireSelection(int selectionIndex, RealTable rootRealTable) {
        BaseQueryExportSelection selection = selection(selectionIndex, rootRealTable);
        if (selection instanceof BaseQueryExportCollectorSelection) {
            return (BaseQueryExportCollectorSelection) selection;
        }
        BaseQueryExportCollectorSelection collectorSelection =
                new BaseQueryExportCollectorSelection(this, selectionIndex, rootRealTable);
        selectionMap.put(selectionIndex, collectorSelection);
        return collectorSelection;
    }

    public BaseQueryExportSelection selectionOrNull(int selectionIndex, RealTable rootRealTable) {
        if (!selectionExportMap.containsKey(selectionIndex)) {
            return null;
        }
        return selection(selectionIndex, rootRealTable);
    }

    BaseQueryExportSelection selectionOrNull(int selectionIndex) {
        if (!selectionExportMap.containsKey(selectionIndex)) {
            return null;
        }
        BaseQueryExportSelection selection = selectionMap.get(selectionIndex);
        return selection != null ? selection : selection(selectionIndex, null);
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

    int nextColumnIndex() {
        return scope.colNo();
    }

    void copyMissingFrom(BaseQueryExport source) {
        for (Map.Entry<Integer, SelectionExport> e : source.selectionExportMap.entrySet()) {
            requireSelectionExport(e.getKey()).copyMissingFrom(e.getValue());
        }
    }

    void overwriteFrom(BaseQueryExport source) {
        for (Map.Entry<Integer, SelectionExport> e : source.selectionExportMap.entrySet()) {
            requireSelectionExport(e.getKey()).overwriteFrom(e.getValue());
        }
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

        private final Map<BaseQueryExportColumn.Key, BaseQueryExportColumn> columnMap = new LinkedHashMap<>();

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

        BaseQueryExportColumn formula(List<RealTable.Key> tableKeys, FormulaTemplate formula) {
            BaseQueryExportColumn.Key key =
                    new BaseQueryExportColumn.Key(tableKeys, null, formula, false);
            BaseQueryExportColumn column = columnMap.get(key);
            if (column == null) {
                column = new BaseQueryExportColumn(tableKeys, formula, nextColumnIndex());
                columnMap.put(key, column);
            }
            return column;
        }

        BaseQueryExportColumn formulaOrNull(List<RealTable.Key> tableKeys, FormulaTemplate formula) {
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

        void copyMissingFrom(SelectionExport source) {
            for (BaseQueryExportColumn column : source.columnMap.values()) {
                columnMap.putIfAbsent(column.key(), column);
            }
            if (expressionIndex == null) {
                expressionIndex = source.expressionIndex;
            }
        }

        void overwriteFrom(SelectionExport source) {
            for (BaseQueryExportColumn column : source.columnMap.values()) {
                columnMap.put(column.key(), column);
            }
            expressionIndex = source.expressionIndex;
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
