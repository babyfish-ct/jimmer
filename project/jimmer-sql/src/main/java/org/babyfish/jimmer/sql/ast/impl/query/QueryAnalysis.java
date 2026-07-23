package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExports;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionAliasRender;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliases;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class QueryAnalysis {

    private final JoinRequirements joinRequirements;

    private final TableUsages tableUsages;

    private final JoinedTypeBranchTableUsages joinedTypeBranchTableUsages;

    private final TableAliases tableAliases;

    private final BaseQueryExports baseQueryExports;

    private final CteTableDependencies cteTableDependencies;

    QueryAnalysis(
            JoinRequirements joinRequirements,
            TableUsages tableUsages,
            JoinedTypeBranchTableUsages joinedTypeBranchTableUsages,
            TableAliases tableAliases,
            BaseQueryExports baseQueryExports,
            CteTableDependencies cteTableDependencies
    ) {
        this.joinRequirements = joinRequirements;
        this.tableUsages = tableUsages;
        this.joinedTypeBranchTableUsages = joinedTypeBranchTableUsages;
        this.tableAliases = tableAliases;
        this.baseQueryExports = baseQueryExports;
        this.cteTableDependencies = cteTableDependencies;
    }

    @Nullable
    BaseQueryExportSelection getBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        return baseQueryExports.exportSelection(baseTableOwner);
    }

    @Nullable
    BaseSelectionAliasRender getBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return baseQueryExports.baseSelectionRender(query);
    }

    @Nullable
    JoinType getRequiredJoinType(TableImplementor<?> table) {
        return joinRequirements.get(table);
    }

    boolean isJoinedTypeBranchTableRequired(TableImplementor<?> table, ImmutableType stageType) {
        return joinedTypeBranchTableUsages.isRequired(table, stageType);
    }

    Set<ImmutableType> getJoinedTypeBranchTableTypes(TableImplementor<?> table) {
        return joinedTypeBranchTableUsages.stageTypes(table);
    }

    TableAliases getTableAliases() {
        return tableAliases;
    }

    List<CteTableDeclaration> getCteTableDeclarations(AbstractMutableStatementImpl statement) {
        return cteTableDependencies.declarations(statement);
    }

    void applyTo(AstContext astContext) {
        if (tableAliases == TableAliases.EMPTY) {
            return;
        }
        tableUsages.applyUsedStatesTo(astContext);
        tableUsages.bindAliases(astContext, tableAliases);
    }
}
