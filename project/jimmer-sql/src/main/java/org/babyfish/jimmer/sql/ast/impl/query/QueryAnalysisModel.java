package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExports;

final class QueryAnalysisModel {

    private final JoinRequirements joinRequirements;

    private final BaseQueryExportUsages baseQueryExportUsages;

    private final TableUsages tableUsages;

    private final BaseQueryExports baseQueryExports;

    QueryAnalysisModel(
            JoinRequirements joinRequirements,
            BaseQueryExportUsages baseQueryExportUsages,
            TableUsages tableUsages,
            BaseQueryExports baseQueryExports
    ) {
        this.joinRequirements = joinRequirements;
        this.baseQueryExportUsages = baseQueryExportUsages;
        this.tableUsages = tableUsages;
        this.baseQueryExports = baseQueryExports;
    }

    JoinRequirements getJoinRequirements() {
        return joinRequirements;
    }

    BaseQueryExportUsages getBaseQueryExportUsages() {
        return baseQueryExportUsages;
    }

    TableUsages getTableUsages() {
        return tableUsages;
    }

    BaseQueryExports getBaseQueryExports() {
        return baseQueryExports;
    }
}
