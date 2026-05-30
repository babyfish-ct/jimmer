package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
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
            BaseQueryExportSelection exportSelection = renderContext.getBaseQueryExportSelection(owner);
            RealTable realTable = TableProxies
                    .resolve(expression.getTable(), ctx)
                    .realTable(renderContext);
            String columnName = getColumnName();
            if (exportSelection != null && columnName != null) {
                if (!exportSelection.isTableBacked()) {
                    builder
                            .sql(exportSelection.getAlias())
                            .sql(".c")
                            .sql(Integer.toString(exportSelection.expressionIndex()));
                    return;
                }
                if (!canReadExportColumn(exportSelection, realTable, builder)) {
                    raw.metadata().renderTo(builder);
                    return;
                }
                Integer index = exportSelection.columnIndexOrNull(realTable, columnName, isForeignKey());
                if (index == null && realTable.getParent() != null) {
                    columnName = foreignKeyColumnName(realTable, columnName, builder);
                    index = exportSelection.columnIndexOrNull(
                            realTable.getParent(),
                            columnName,
                            isForeignKey()
                    );
                }
                if (index != null) {
                    builder.sql(exportSelection.getAlias()).sql(".c").sql(Integer.toString(index));
                    return;
                }
            }
            raw.metadata().renderTo(builder);
        } finally {
            ctx.popStatement();
        }
    }

    private boolean canReadExportColumn(
            BaseQueryExportSelection exportSelection,
            RealTable realTable,
            AbstractSqlBuilder<?> builder
    ) {
        if (exportSelection.isRootTable(realTable)) {
            return true;
        }
        if (!isForeignKey() ||
                realTable.getParent() == null ||
                !(realTable.getTableLikeImplementor() instanceof TableImplementor<?>)) {
            return false;
        }
        TableImplementor<?> table = (TableImplementor<?>) realTable.getTableLikeImplementor();
        ImmutableProp joinProp = table.getJoinProp();
        return getValueProp().isId() &&
                joinProp != null &&
                !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                TableUtils.isRawIdAllowed(table, builder.sqlClient()) &&
                !table.isInverse() &&
                !joinProp.isMiddleTableDefinition() &&
                exportSelection.containsTable(realTable.getParent());
    }

    private String foreignKeyColumnName(
            RealTable realTable,
            String targetColumnName,
            AbstractSqlBuilder<?> builder
    ) {
        if (!isForeignKey() ||
                !(realTable.getTableLikeImplementor() instanceof TableImplementor<?>)) {
            return targetColumnName;
        }
        TableImplementor<?> table = (TableImplementor<?>) realTable.getTableLikeImplementor();
        ImmutableProp joinProp = table.getJoinProp();
        if (joinProp == null || table.isInverse() || !joinProp.isColumnDefinition()) {
            return targetColumnName;
        }
        ColumnDefinition targetDefinition = expression
                .getProp()
                .getStorage(builder.sqlClient().getMetadataStrategy());
        int index = targetDefinition.index(targetColumnName);
        if (index == -1) {
            return targetColumnName;
        }
        ColumnDefinition parentDefinition = joinProp.getStorage(builder.sqlClient().getMetadataStrategy());
        return parentDefinition.name(index);
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
