package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExports;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliases;

final class QueryAnalysisModel {

    private final JoinRequirements joinRequirements;

    private final BaseQueryExportUsages baseQueryExportUsages;

    private final TableUsages tableUsages;

    private final TableAliases tableAliases;

    private final BaseQueryExports baseQueryExports;

    QueryAnalysisModel(
            JoinRequirements joinRequirements,
            BaseQueryExportUsages baseQueryExportUsages,
            TableUsages tableUsages,
            TableAliases tableAliases,
            BaseQueryExports baseQueryExports
    ) {
        this.joinRequirements = joinRequirements;
        this.baseQueryExportUsages = baseQueryExportUsages;
        this.tableUsages = tableUsages;
        this.tableAliases = tableAliases;
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

    TableAliases getTableAliases() {
        return tableAliases;
    }

    BaseQueryExports getBaseQueryExports() {
        return baseQueryExports;
    }
}
