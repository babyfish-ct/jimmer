package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;

public class ApplyFilterVisitor extends AstVisitor {

    private final FilterLevel level;

    public ApplyFilterVisitor(AstContext ctx, FilterLevel level) {
        super(ctx);
        this.level = level;
    }

    @Override
    public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
        return true;
    }

    @Override
    public void visitTableReference(TableImplementor<?> table, ImmutableProp prop, boolean rawId) {
        AstContext ctx = getAstContext();
        if (prop != null && prop.isId() && (rawId || table.isRawIdAllowed(ctx.getSqlClient()))) {
            table = table.getParent();
        }
        while (table != null) {
            table.getStatement().applyGlobalFiler(table, level);
            table = table.getParent();
        }
    }
}
