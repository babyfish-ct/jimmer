package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.QueryAnalysisContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.*;

final class BaseQueryScope {

    final QueryAnalysisContext ctx;

    private final Map<RealTable, BaseQueryExport> exportMap =
            new LinkedHashMap<>();

    private int colNoSequence;

    BaseQueryScope(QueryAnalysisContext ctx) {
        this.ctx = ctx;
    }

    BaseQueryExportCollectorSelection requireExportSelection(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = ctx.resolveBaseTable(baseTableOwner.getBaseTable());
        RealTable realBaseTable = ctx.realTable(baseTable);
        BaseQueryExport export = exportMap.get(realBaseTable);
        if (export == null) {
            export = new BaseQueryExport(this, realBaseTable);
            exportMap.put(realBaseTable, export);
        }
        return export.requireSelection(baseTableOwner.index, rootRealTable(baseTable, baseTableOwner.index));
    }

    BaseQueryExportSelection exportSelectionOrNull(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = ctx.resolveBaseTable(baseTableOwner.getBaseTable());
        RealTable realBaseTable = ctx.realTable(baseTable);
        BaseQueryExport export = exportMap.get(realBaseTable);
        return export != null ?
                export.selectionOrNull(baseTableOwner.index, rootRealTable(baseTable, baseTableOwner.index)) :
                null;
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

    BaseQueryExportResolver toResolver() {
        return new BaseQueryExportResolver(ctx, exportMap);
    }
}
