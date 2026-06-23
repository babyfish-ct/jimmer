package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class BaseQueryReadSupport {

    private final QueryRenderContext renderContext;

    public BaseQueryReadSupport(QueryRenderContext renderContext) {
        this.renderContext = renderContext;
    }

    public void requireSelection(BaseTableOwner owner) {
        Objects.requireNonNull(
                selection(owner),
                "No base-query export selection is available for " + owner
        );
    }

    @Nullable
    public BaseQueryRead expression(BaseTableOwner owner) {
        BaseQueryExportSelection selection = selection(owner);
        return selection != null ? read(selection, selection.expressionIndex()) : null;
    }

    @Nullable
    public BaseQueryRead propExpression(
            BaseTableOwner owner,
            PropExpressionImplementor<?> expression,
            RealTable realTable,
            MetadataStrategy strategy
    ) {
        BaseQueryExportSelection selection = selection(owner);
        if (selection == null) {
            return null;
        }
        RealTable table = null;
        if (selection.isRootTable(realTable)) {
            table = realTable;
        } else if (owner.equals(BaseTableOwner.of(expression.getTable())) && selection.isTableBacked()) {
            table = selection.getRootRealTable();
        }
        if (table == null) {
            return null;
        }
        SqlTemplate template = expression.getProp().getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            return read(selection, selection.formulaIndex(table, (FormulaTemplate) template));
        }
        if (!expression.getProp().isColumnDefinition()) {
            return read(selection, selection.expressionIndex());
        }
        return columns(selection, table, expression.getProp().getStorage(strategy), false);
    }

    @Nullable
    public BaseQueryRead valueGetter(
            BaseTableOwner owner,
            PropExpressionImplementor<?> expression,
            RealTable realTable,
            ImmutableProp valueProp,
            @Nullable String columnName,
            boolean foreignKey,
            JSqlClientImplementor sqlClient
    ) {
        BaseQueryExportSelection selection = selection(owner);
        if (selection == null || columnName == null) {
            return null;
        }
        if (!selection.isTableBacked()) {
            return read(selection, selection.expressionIndex());
        }
        if (!canReadExportColumn(selection, realTable, valueProp, foreignKey, sqlClient)) {
            return null;
        }
        Integer index = selection.columnIndexOrNull(realTable, columnName, foreignKey);
        if (index == null && realTable.getParent() != null) {
            columnName = foreignKeyColumnName(expression, realTable, columnName, foreignKey, sqlClient);
            index = selection.columnIndexOrNull(realTable.getParent(), columnName, foreignKey);
        }
        return index != null ? read(selection, index) : null;
    }

    @Nullable
    public BaseQueryRead column(
            BaseTableOwner owner,
            RealTable table,
            String columnName,
            boolean foreignKeyInBaseQuery
    ) {
        BaseQueryExportSelection selection = selection(owner);
        return selection != null ?
                read(selection, selection.columnIndex(table, columnName, foreignKeyInBaseQuery)) :
                null;
    }

    @Nullable
    public BaseQueryRead formula(BaseTableOwner owner, RealTable table, FormulaTemplate formula) {
        BaseQueryExportSelection selection = selection(owner);
        return selection != null ? read(selection, selection.formulaIndex(table, formula)) : null;
    }

    public boolean canReadSelection(
            BaseTableOwner owner,
            RealTable table,
            ImmutableProp prop,
            @Nullable ImmutableProp joinProp,
            boolean rawId,
            boolean idViewAllowed,
            JSqlClientImplementor sqlClient
    ) {
        BaseQueryExportSelection selection = selection(owner);
        return selection != null &&
                (selection.isRootTable(table) ||
                        canReadSelectedIdView(selection, table, prop, joinProp, rawId, idViewAllowed, sqlClient));
    }

    private BaseQueryRead columns(
            BaseQueryExportSelection selection,
            RealTable table,
            ColumnDefinition definition,
            boolean foreignKeyInBaseQuery
    ) {
        int size = definition.size();
        int[] indexes = new int[size];
        for (int i = 0; i < size; i++) {
            indexes[i] = selection.columnIndex(table, definition.name(i), foreignKeyInBaseQuery);
        }
        return new BaseQueryRead(selection.getRealBaseTable(), indexes);
    }

    private BaseQueryRead read(BaseQueryExportSelection selection, int index) {
        return new BaseQueryRead(selection.getRealBaseTable(), index);
    }

    @Nullable
    private BaseQueryExportSelection selection(BaseTableOwner owner) {
        return renderContext.getBaseQueryExportSelection(owner);
    }

    private boolean canReadExportColumn(
            BaseQueryExportSelection selection,
            RealTable realTable,
            ImmutableProp valueProp,
            boolean foreignKey,
            JSqlClientImplementor sqlClient
    ) {
        if (selection.isRootTable(realTable)) {
            return true;
        }
        if (!foreignKey ||
                realTable.getParent() == null ||
                !(realTable.getTableLikeImplementor() instanceof TableImplementor<?>)) {
            return false;
        }
        TableImplementor<?> table = (TableImplementor<?>) realTable.getTableLikeImplementor();
        ImmutableProp joinProp = table.getJoinProp();
        return valueProp.isId() &&
                joinProp != null &&
                !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                TableUtils.isRawIdAllowed(table, sqlClient) &&
                !table.isInverse() &&
                !joinProp.isMiddleTableDefinition() &&
                selection.containsTable(realTable.getParent());
    }

    private boolean canReadSelectedIdView(
            BaseQueryExportSelection selection,
            RealTable table,
            ImmutableProp prop,
            @Nullable ImmutableProp joinProp,
            boolean rawId,
            boolean idViewAllowed,
            JSqlClientImplementor sqlClient
    ) {
        if (!(table.getTableLikeImplementor() instanceof TableImplementor<?>)) {
            return false;
        }
        TableImplementor<?> tableImplementor = (TableImplementor<?>) table.getTableLikeImplementor();
        return prop.isId() &&
                joinProp != null &&
                !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || idViewAllowed && TableUtils.isRawIdAllowed(tableImplementor, sqlClient)) &&
                !tableImplementor.isInverse() &&
                table.getParent() != null &&
                selection.containsTable(table.getParent());
    }

    private String foreignKeyColumnName(
            PropExpressionImplementor<?> expression,
            RealTable realTable,
            String targetColumnName,
            boolean foreignKey,
            JSqlClientImplementor sqlClient
    ) {
        if (!foreignKey ||
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
                .getStorage(sqlClient.getMetadataStrategy());
        int index = targetDefinition.index(targetColumnName);
        if (index == -1) {
            return targetColumnName;
        }
        ColumnDefinition parentDefinition = joinProp.getStorage(sqlClient.getMetadataStrategy());
        return parentDefinition.name(index);
    }
}
