package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.QueryAnalysisContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.*;

final class BaseQueryScope {

    final QueryAnalysisContext ctx;

    private final Map<RealTable, BaseQueryExport> exportMap = new LinkedHashMap<>();

    private int colNoSequence;

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
            export = new BaseQueryExport(this, realBaseTable);
            exportMap.put(realBaseTable, export);
        }
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

    int colNo() {
        return ++colNoSequence;
    }

    RealTable rootRealTable(BaseTableImplementor baseTable, int selectionIndex) {
        Selection<?> selection = baseTable.getSelections().get(selectionIndex);
        if (!(selection instanceof Table<?>)) {
            return null;
        }
        return ctx.realTable(ctx.resolve((Table<?>) selection));
    }

    private BaseQueryExport export(BaseTableSymbol baseTableSymbol) {
        BaseTableImplementor baseTable = ctx.resolveBaseTable(baseTableSymbol);
        if (baseTable == null) {
            return null;
        }
        RealTable realBaseTable = ctx.realTable(baseTable);
        BaseQueryExport export = exportMap.get(realBaseTable);
        if (export == null) {
            export = new BaseQueryExport(this, realBaseTable);
            exportMap.put(realBaseTable, export);
        }
        return export;
    }

    private BaseQueryExport exportOrNull(BaseTableSymbol baseTableSymbol) {
        BaseTableImplementor baseTable = ctx.resolveBaseTable(baseTableSymbol);
        return baseTable != null ? exportMap.get(ctx.realTable(baseTable)) : null;
    }

    BaseQueryExportResolver toResolver() {
        return new BaseQueryExportResolver(ctx, exportMap);
    }
}
