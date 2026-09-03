package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal bridge that lets mutation statements reuse the normal base-query analysis.
 */
public final class MutationQueryAnalyzer {

    private MutationQueryAnalyzer() {
    }

    public static void apply(
            SqlBuilder builder,
            TypedQueryImplementor mutation,
            TypedBaseQueryImplementor<?> source
    ) {
        AstContext astContext = builder.getAstContext();
        QueryAnalyzer analyzer = new QueryAnalyzer(astContext, mutation);
        List<ConfigurableBaseQueryImpl<?>> queries = new ArrayList<>();
        source.collectConfigurableQueries(queries);
        // Join requirements must be analyzed after virtual predicates have been resolved.
        // Do this per branch because a merged query can contain both frozen and mutable branches.
        for (ConfigurableBaseQueryImpl<?> query : queries) {
            MutableBaseQueryImpl mutableQuery = query.getMutableQuery();
            if (!mutableQuery.isFrozen()) {
                mutableQuery.applyVirtualPredicates(astContext);
            }
        }
        StatementContext sourceContext = source.firstConfigurableQuery().getMutableQuery().getContext();
        source.applyGlobalFilters(
                astContext,
                sourceContext != null ? sourceContext.getFilterLevel() : FilterLevel.DEFAULT,
                analyzer.analyzeJoinRequirements()
        );
        builder.setQueryAnalysis(analyzer.analyze());
    }
}
