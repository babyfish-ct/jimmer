package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryRead;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class BaseTableValueGetter implements ValueGetter, GetterMetadata {

    private final BaseTableOwner owner;

    private final PropExpressionImplementor<?> expression;

    private final ValueGetter raw;

    BaseTableValueGetter(
            BaseTableOwner owner,
            PropExpressionImplementor<?> expression,
            ValueGetter raw
    ) {
        this.owner = owner;
        this.expression = expression;
        this.raw = raw;
    }

    @Override
    public Object get(Object value) {
        return raw.get(value);
    }

    @Override
    public GetterMetadata metadata() {
        return this;
    }

    @Override
    public ImmutableProp getValueProp() {
        return raw.metadata().getValueProp();
    }

    @Override
    public @Nullable String getColumnName() {
        return raw.metadata().getColumnName();
    }

    @Override
    public boolean isForeignKey() {
        return raw.metadata().isForeignKey();
    }

    @Override
    public boolean isNullable() {
        return raw.metadata().isNullable();
    }

    @Override
    public boolean isJson() {
        return raw.metadata().isJson();
    }

    @Override
    public boolean hasDefaultValue() {
        return raw.metadata().hasDefaultValue();
    }

    @Override
    public Object getDefaultValue() {
        return raw.metadata().getDefaultValue();
    }

    @Override
    public Class<?> getSqlType() {
        return raw.metadata().getSqlType();
    }

    @Override
    public String getSqlTypeName() {
        return raw.metadata().getSqlTypeName();
    }

    @Override
    public void renderTo(AbstractSqlBuilder<?> builder) {
        AstContext ctx = builder.assertSimple().getAstContext();
        QueryRenderContext renderContext = builder.assertSimple().getQueryRenderContext();
        ctx.pushStatement(owner.getBaseTable().getQuery().getMutableQuery());
        try {
            RealTable realTable = TableProxies
                    .resolve(expression.getTable(), ctx)
                    .realTable(renderContext);
            BaseQueryRead read = renderContext.getBaseQueryReadSupport().valueGetter(
                    owner,
                    expression,
                    realTable,
                    getValueProp(),
                    getColumnName(),
                    isForeignKey(),
                    builder.sqlClient()
            );
            if (read != null) {
                builder
                        .sql(builder.assertSimple().alias(read.getRealBaseTable()))
                        .sql(".c")
                        .sql(Integer.toString(read.index(0)));
                return;
            }
            raw.metadata().renderTo(builder);
        } finally {
            ctx.popStatement();
        }
    }

    static List<ValueGetter> wrap(
            BaseTableOwner owner,
            PropExpressionImplementor<?> expression,
            List<ValueGetter> rawGetters
    ) {
        List<ValueGetter> getters = new ArrayList<>(rawGetters.size());
        for (ValueGetter rawGetter : rawGetters) {
            getters.add(new BaseTableValueGetter(owner, expression, rawGetter));
        }
        return getters;
    }
}
