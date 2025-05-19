package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.BaseTableQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseTableQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBaseTable<T> implements BaseTableImplementor<T> {

    private final BaseTableQueryImplementor<T, ?> query;

    private final RealTable realTable;

    protected AbstractBaseTable(BaseTableQueryImplementor<T, ?> query) {
        this.query = query;
        this.realTable = new RealTableImpl(this);
    }

    @Override
    public BaseTableQueryImplementor<T, ?> getQuery() {
        return query;
    }

    @Override
    public AbstractMutableStatementImpl getStatement() {
        return ((ConfigurableBaseTableQueryImpl<?, ?, ?>)query).getBaseQuery();
    }

    @Override
    public ImmutableProp getJoinProp() {
        return null;
    }

    @Override
    public RealTable realTable(JoinTypeMergeScope scope) {
        return realTable;
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        AstContext astContext = builder.assertSimple().getAstContext();
        astContext.pushStatement(getStatement());
        builder.sql("from (");
        query.renderTo(builder);
        builder.sql(") ").sql(realTable.getAlias());
        astContext.popStatement();
    }
}
