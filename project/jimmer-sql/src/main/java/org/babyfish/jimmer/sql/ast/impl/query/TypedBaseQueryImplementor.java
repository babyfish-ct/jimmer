package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableSelectionLayout;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableShape;

import java.util.List;

public interface TypedBaseQueryImplementor<T extends BaseTable>
        extends TypedBaseQuery<T>, TypedQueryImplementor {

    TableImplementor<?> resolveRootTable(Table<?> table);

    ConfigurableBaseQueryImpl<?> firstConfigurableQuery();

    void collectConfigurableQueries(List<ConfigurableBaseQueryImpl<?>> queries);

    void collectCteDependencyQueries(List<ConfigurableBaseQueryImpl<?>> queries);

    void applyGlobalFilters(AstContext astContext, FilterLevel level, QueryAnalysis queryAnalysis);

    default void acceptBaseTableReference(AstVisitor visitor) {
        MergedBaseQueryImpl<?> mergedBy = getMergedBy();
        (mergedBy != null ? mergedBy : this).accept(visitor);
    }

    MergedBaseQueryImpl<T> getMergedBy();

    void setMergedBy(MergedBaseQueryImpl<T> mergedBaseQuery);

    BaseTableShape<?, ?> getBaseTableShape();

    BaseTableSymbol asBaseTableSymbol(BaseTableSelectionLayout selectionLayout, boolean cte);

    T asBaseTable(BaseTableSelectionLayout selectionLayout, boolean cte);
}
