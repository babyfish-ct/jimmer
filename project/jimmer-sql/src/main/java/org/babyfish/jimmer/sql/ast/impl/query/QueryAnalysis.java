package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
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

    private final AstContext astContext;

    private final QueryAnalysisModel model;

    QueryAnalysis(AstContext astContext, QueryAnalysisModel model) {
        this.astContext = astContext;
        this.model = model;
    }

    @Nullable
    BaseQueryExportSelection getBaseQueryExportSelection(BaseTableOwner baseTableOwner) {
        BaseQueryExportSelection selection = model.getBaseQueryExports().exportSelection(baseTableOwner);
        if (selection != null) {
            return selection;
        }
        return model.getBaseQueryExports().exportSelection(astContext.resolveBaseTableOwner(baseTableOwner));
    }

    @Nullable
    BaseSelectionAliasRender getBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return model.getBaseQueryExports().baseSelectionRender(query);
    }

    @Nullable
    JoinType getRequiredJoinType(TableImplementor<?> table) {
        return model.getJoinRequirements().get(table);
    }

    boolean isJoinedTypeBranchTableRequired(TableImplementor<?> table, ImmutableType stageType) {
        return model.getJoinedTypeBranchTableUsages().isRequired(table, stageType);
    }

    Set<ImmutableType> getJoinedTypeBranchTableTypes(TableImplementor<?> table) {
        return model.getJoinedTypeBranchTableUsages().stageTypes(table);
    }

    TableAliases getTableAliases() {
        return model.getTableAliases();
    }

    List<CteTableDeclaration> getCteTableDeclarations(AbstractMutableStatementImpl statement) {
        return model.getCteTableDependencies().declarations(statement);
    }
}
