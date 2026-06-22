package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExports;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliases;

final class QueryAnalysisModel {

    private final JoinRequirements joinRequirements;

    private final BaseQueryExportUsages baseQueryExportUsages;

    private final TableUsages tableUsages;

    private final TableAliases tableAliases;

    private final BaseQueryExports baseQueryExports;

    private final CteTableDependencies cteTableDependencies;

    QueryAnalysisModel(
            JoinRequirements joinRequirements,
            BaseQueryExportUsages baseQueryExportUsages,
            TableUsages tableUsages,
            TableAliases tableAliases,
            BaseQueryExports baseQueryExports,
            CteTableDependencies cteTableDependencies
    ) {
        this.joinRequirements = joinRequirements;
        this.baseQueryExportUsages = baseQueryExportUsages;
        this.tableUsages = tableUsages;
        this.tableAliases = tableAliases;
        this.baseQueryExports = baseQueryExports;
        this.cteTableDependencies = cteTableDependencies;
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

    CteTableDependencies getCteTableDependencies() {
        return cteTableDependencies;
    }
}
