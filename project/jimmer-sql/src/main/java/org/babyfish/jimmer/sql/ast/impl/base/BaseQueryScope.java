package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.QueryAnalysisContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.*;

final class BaseQueryScope {

    final QueryAnalysisContext ctx;

    private final Map<RealTable, BaseQueryExport> exportMap = new LinkedHashMap<>();

    private final Map<BaseTableSymbol, BaseQueryExport> exportMapByBaseTable =
            new IdentityHashMap<>();

    private final BaseQueryExport.ColumnIndexSequence columnIndexSequence =
            new BaseQueryExport.ColumnIndexSequence();

    BaseQueryScope(QueryAnalysisContext ctx) {
        this.ctx = ctx;
    }

    BaseQueryExportCollectorSelection requireExportSelection(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = ctx.resolveBaseTable(baseTableOwner.getBaseTable());
        if (baseTable == null) {
            return null;
        }
        RealTable realBaseTable = ctx.realTable(baseTable);
        BaseQueryExport export = exportMap.get(realBaseTable);
        if (export == null) {
            export = new BaseQueryExport(columnIndexSequence, realBaseTable);
            exportMap.put(realBaseTable, export);
        }
        exportMapByBaseTable.put(baseTableOwner.getBaseTable(), export);
        return export.requireSelection(baseTableOwner.index, rootRealTable(baseTable, baseTableOwner.index));
    }

    void synchronizeMergedExports(List<BaseTableSymbol> baseTables) {
        BaseQueryExport canonical = null;
        for (BaseTableSymbol baseTable : baseTables) {
            BaseQueryExport export = exportOrNull(baseTable);
            if (export == null) {
                continue;
            }
            if (canonical == null) {
                canonical = export;
            } else {
                canonical.copyMissingFrom(export);
            }
        }
        if (canonical == null) {
            return;
        }
        for (BaseTableSymbol baseTable : baseTables) {
            BaseQueryExport export = export(baseTable);
            if (export != null) {
                export.overwriteFrom(canonical);
            }
        }
    }

    RealTable rootRealTable(BaseTableImplementor baseTable, int selectionIndex) {
        Selection<?> selection = baseTable.getSelections().get(selectionIndex);
        if (!(selection instanceof Table<?>)) {
            return null;
        }
        Table<?> table = (Table<?>) selection;
        BaseTableOwner owner = BaseTableOwner.of(table);
        return ctx.realTable(owner != null ? ctx.resolve(owner, table) : ctx.resolve(table));
    }

    private BaseQueryExport export(BaseTableSymbol baseTableSymbol) {
        BaseTableImplementor baseTable = ctx.resolveBaseTable(baseTableSymbol);
        if (baseTable == null) {
            return null;
        }
        RealTable realBaseTable = ctx.realTable(baseTable);
        BaseQueryExport export = exportMap.get(realBaseTable);
        if (export == null) {
            export = new BaseQueryExport(columnIndexSequence, realBaseTable);
            exportMap.put(realBaseTable, export);
        }
        exportMapByBaseTable.put(baseTableSymbol, export);
        return export;
    }

    BaseQueryExport exportOrNull(BaseTableSymbol baseTableSymbol) {
        BaseQueryExport export = exportMapByBaseTable.get(baseTableSymbol);
        if (export != null) {
            return export;
        }
        BaseTableImplementor baseTable = ctx.resolveBaseTable(baseTableSymbol);
        if (baseTable == null) {
            return null;
        }
        export = exportMap.get(ctx.realTable(baseTable));
        if (export != null) {
            exportMapByBaseTable.put(baseTableSymbol, export);
        }
        return export;
    }

    void prepareExports() {
        for (BaseQueryExport export : exportMap.values()) {
            export.prepareSelections(this);
        }
    }
}
