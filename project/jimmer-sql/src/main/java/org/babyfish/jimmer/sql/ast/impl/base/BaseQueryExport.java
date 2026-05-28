package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.ast.Selection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BaseQueryExport {

    private final BaseQueryScope scope;

    private final RealTable realBaseTable;

    private final Map<Integer, BaseSelectionMapper> selectionMapperMap =
            new LinkedHashMap<>();

    private final Map<Integer, SelectionExport> selectionExportMap =
            new LinkedHashMap<>();

    private final Map<Integer, BaseQueryExportSelection> selectionMap =
            new LinkedHashMap<>();

    private boolean frozen;

    BaseQueryExport(BaseQueryScope scope, RealTable realBaseTable) {
        this.scope = scope;
        this.realBaseTable = realBaseTable;
    }

    public RealTable getRealBaseTable() {
        return realBaseTable;
    }

    public BaseSelectionMapper mapper(int selectionIndex) {
        return selectionMapperMap.computeIfAbsent(
                selectionIndex,
                it -> new BaseSelectionMapper(this, selection(it))
        );
    }

    public BaseSelectionMapper mapperOrNull(int selectionIndex) {
        return selectionMapperMap.get(selectionIndex);
    }

    BaseQueryExportColumn column(
            BaseQueryExportSelection selection,
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery
    ) {
        return selectionExport(selection.getIndex()).column(
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

    BaseQueryExportColumn joinKeyColumn(
            BaseQueryExportSelection selection,
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery
    ) {
        return selectionExport(selection.getIndex()).column(
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
        return selectionExport(selection.getIndex()).formula(tableKeys, formula);
    }

    Collection<BaseQueryExportColumn> columns(int selectionIndex) {
        SelectionExport export = selectionExportMap.get(selectionIndex);
        return export != null ? export.columns() : java.util.Collections.emptyList();
    }

    int expressionIndex(BaseQueryExportSelection selection) {
        return selectionExport(selection.getIndex()).expressionIndex();
    }

    void freeze() {
        if (!frozen) {
            frozen = true;
            for (SelectionExport export : selectionExportMap.values()) {
                export.freeze();
            }
        }
    }

    AstContext astContext() {
        return scope.astContext;
    }

    int nextColumnIndex() {
        return scope.colNo();
    }

    private SelectionExport selectionExport(int selectionIndex) {
        SelectionExport export = selectionExportMap.get(selectionIndex);
        if (export == null) {
            if (frozen || scope.isFrozen()) {
                throw unresolved(selectionIndex);
            }
            export = new SelectionExport(selectionIndex);
            selectionExportMap.put(selectionIndex, export);
        }
        return export;
    }

    private BaseQueryExportSelection selection(int selectionIndex) {
        return selectionMap.computeIfAbsent(
                selectionIndex,
                it -> {
                    Selection<?> selection = ((BaseTableImplementor) realBaseTable.getTableLikeImplementor())
                            .getSelections()
                            .get(it);
                    return new BaseQueryExportSelection(this, it, selection);
                }
        );
    }

    private IllegalStateException unresolved(int selectionIndex) {
        return new IllegalStateException(
                "Base query export selection #" +
                        selectionIndex +
                        " of " +
                        realBaseTable +
                        " was not resolved during QueryAnalysis"
        );
    }

    private final class SelectionExport {

        private final int selectionIndex;

        private final Map<BaseQueryExportColumn.Key, BaseQueryExportColumn> columnMap =
                new LinkedHashMap<>();

        private Integer expressionIndex;

        private boolean frozen;

        private SelectionExport(int selectionIndex) {
            this.selectionIndex = selectionIndex;
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
                checkNotFrozen();
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
                checkNotFrozen();
                column = new BaseQueryExportColumn(
                        tableKeys,
                        formula,
                        nextColumnIndex()
                );
                columnMap.put(key, column);
            }
            return column;
        }

        int expressionIndex() {
            Integer index = expressionIndex;
            if (index == null) {
                checkNotFrozen();
                expressionIndex = index = nextColumnIndex();
            }
            return index;
        }

        void freeze() {
            frozen = true;
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

        private void checkNotFrozen() {
            if (frozen || BaseQueryExport.this.frozen || scope.isFrozen()) {
                throw unresolved(selectionIndex);
            }
        }
    }
}
