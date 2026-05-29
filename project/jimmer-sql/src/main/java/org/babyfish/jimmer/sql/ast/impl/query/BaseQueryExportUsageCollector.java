package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.jetbrains.annotations.Nullable;

final class BaseQueryExportUsageCollector extends AstVisitor {

    private final BaseQueryExportUsages.Builder builder =
            new BaseQueryExportUsages.Builder();

    private BaseQueryExportUsageCollector(AstContext ctx, QueryAnalysis queryAnalysis) {
        super(ctx, queryAnalysis);
    }

    static BaseQueryExportUsages collect(AstContext ctx, Ast ast, QueryAnalysis queryAnalysis) {
        BaseQueryExportUsageCollector collector = new BaseQueryExportUsageCollector(ctx, queryAnalysis);
        ast.accept(collector);
        return collector.builder.build();
    }

    @Override
    public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
        if (prop != null) {
            return;
        }
        BaseTableOwner baseTableOwner = table.getBaseTableOwner();
        if (baseTableOwner != null) {
            builder.requireFullRowExport(baseTableOwner);
        }
    }
}
