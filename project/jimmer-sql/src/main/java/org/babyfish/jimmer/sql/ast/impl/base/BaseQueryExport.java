package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

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
                it -> new BaseSelectionMapper(this, it)
        );
    }

    public BaseSelectionMapper mapperOrNull(int selectionIndex) {
        return selectionMapperMap.get(selectionIndex);
    }

    BaseQueryExportColumn column(
            int selectionIndex,
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery
    ) {
        return selectionExport(selectionIndex).column(
                tableKeys,
                name,
                foreignKeyInBaseQuery,
                BaseQueryExportColumnRole.SELECTION
        );
    }

    BaseQueryExportColumn joinKeyColumn(
            int selectionIndex,
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery
    ) {
        return selectionExport(selectionIndex).column(
                tableKeys,
                name,
                foreignKeyInBaseQuery,
                BaseQueryExportColumnRole.JOIN_KEY
        );
    }

    BaseQueryExportColumn formula(
            int selectionIndex,
            List<RealTable.Key> tableKeys,
            FormulaTemplate formula
    ) {
        return selectionExport(selectionIndex).formula(tableKeys, formula);
    }

    Collection<BaseQueryExportColumn> columns(int selectionIndex) {
        SelectionExport export = selectionExportMap.get(selectionIndex);
        return export != null ? export.columns() : java.util.Collections.emptyList();
    }

    AstContext astContext() {
        return scope.astContext;
    }

    int nextColumnIndex() {
        return scope.colNo();
    }

    private SelectionExport selectionExport(int selectionIndex) {
        return selectionExportMap.computeIfAbsent(
                selectionIndex,
                it -> new SelectionExport()
        );
    }

    private final class SelectionExport {

        private final Map<BaseQueryExportColumn.Key, BaseQueryExportColumn> columnMap =
                new LinkedHashMap<>();

        BaseQueryExportColumn column(
                List<RealTable.Key> tableKeys,
                String name,
                boolean foreignKeyInBaseQuery,
                BaseQueryExportColumnRole role
        ) {
            BaseQueryExportColumn.Key key =
                    new BaseQueryExportColumn.Key(tableKeys, name, null, foreignKeyInBaseQuery);
            return columnMap.computeIfAbsent(
                    key,
                    it -> new BaseQueryExportColumn(
                            tableKeys,
                            name,
                            foreignKeyInBaseQuery,
                            role,
                            nextColumnIndex()
                    )
            );
        }

        BaseQueryExportColumn formula(
                List<RealTable.Key> tableKeys,
                FormulaTemplate formula
        ) {
            BaseQueryExportColumn.Key key =
                    new BaseQueryExportColumn.Key(tableKeys, null, formula, false);
            return columnMap.computeIfAbsent(
                    key,
                    it -> new BaseQueryExportColumn(
                            tableKeys,
                            formula,
                            nextColumnIndex()
                    )
            );
        }

        Collection<BaseQueryExportColumn> columns() {
            return columnMap.values();
        }
    }
}
