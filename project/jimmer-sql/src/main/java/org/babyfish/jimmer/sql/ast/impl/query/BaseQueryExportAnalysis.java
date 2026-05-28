package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryScope;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionMapper;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlTemplate;
import org.jetbrains.annotations.Nullable;

final class BaseQueryExportAnalysis {

    private BaseQueryExportAnalysis() {}

    static void analyze(AbstractMutableStatementImpl statement, AstContext ctx) {
        BaseQueryScope scope = ctx.getBaseQueryScope();
        if (scope == null) {
            return;
        }
        analyze(statement.getTableLikeImplementor(), ctx);
    }

    static void analyzeTableReference(
            RealTable table,
            @Nullable ImmutableProp prop,
            boolean rawId,
            AstContext ctx
    ) {
        BaseSelectionMapper mapper = ctx.getBaseSelectionMapper(table.getBaseTableOwner());
        if (mapper == null) {
            return;
        }
        TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
        if (!(implementor instanceof TableImplementor<?>)) {
            return;
        }
        if (!mapper.isRootTable(table)) {
            return;
        }
        TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
        if (prop == null) {
            for (ImmutableProp selectableProp : tableImplementor.getImmutableType().getSelectableProps().values()) {
                analyzeProp(table, tableImplementor, selectableProp, false, mapper, ctx);
            }
        } else {
            analyzeProp(table, tableImplementor, prop, rawId, mapper, ctx);
        }
    }

    private static void analyze(
            TableLikeImplementor<?> tableLikeImplementor,
            AstContext ctx
    ) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            analyze((BaseTableImplementor) tableLikeImplementor, ctx);
        } else {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) tableLikeImplementor;
            if (tableImplementor.hasBaseTable()) {
                Iterable<TableLikeImplementor<?>> children =
                        (Iterable<TableLikeImplementor<?>>) tableImplementor;
                for (TableLikeImplementor<?> child : children) {
                    analyze(child, ctx);
                }
            }
        }
    }

    private static void analyze(
            BaseTableImplementor baseTableImplementor,
            AstContext ctx
    ) {
        for (Selection<?> selection : baseTableImplementor.toSymbol().getSelections()) {
            if (!(selection instanceof Table<?>)) {
                continue;
            }
            Table<?> table = (Table<?>) selection;
            TableImplementor<?> tableImplementor = TableProxies.resolve(table, ctx);
            BaseSelectionMapper mapper = ctx.getBaseSelectionMapper(tableImplementor.getBaseTableOwner());
            if (mapper == null) {
                continue;
            }
            RealTable realTable = tableImplementor.realTable(ctx);
            for (ImmutableProp prop : tableImplementor.getImmutableType().getSelectableProps().values()) {
                analyzeProp(realTable, tableImplementor, prop, false, mapper, ctx);
            }
            for (RealTable childTable : realTable) {
                if (!(childTable.getTableLikeImplementor() instanceof TableImplementor<?>)) {
                    continue;
                }
                TableImplementor<?> childTableImplementor =
                        (TableImplementor<?>) childTable.getTableLikeImplementor();
                ImmutableProp prop = childTableImplementor.getJoinProp();
                if (prop == null) {
                    break;
                }
                if (childTableImplementor.isInverse()) {
                    prop = prop.getOpposite();
                    if (prop == null) {
                        continue;
                    }
                }
                if (!prop.isColumnDefinition()) {
                    continue;
                }
                ColumnDefinition definition = prop.getStorage(ctx.getSqlClient().getMetadataStrategy());
                int size = definition.size();
                for (int i = 0; i < size; i++) {
                    mapper.joinKeyColumnIndex(realTable.getAlias(), definition.name(i), false);
                }
            }
        }
    }

    private static void analyzeProp(
            RealTable table,
            TableImplementor<?> tableImplementor,
            ImmutableProp prop,
            boolean rawId,
            BaseSelectionMapper mapper,
            AstContext ctx
    ) {
        SqlTemplate template = prop.getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            mapper.formulaIndex(table.getAlias(), (FormulaTemplate) template);
            return;
        }
        if (!prop.isColumnDefinition()) {
            return;
        }
        MetadataStrategy strategy = ctx.getSqlClient().getMetadataStrategy();
        ImmutableProp joinProp = tableImplementor.getJoinProp();
        if (prop.isId() &&
                joinProp != null &&
                !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(tableImplementor, ctx.getSqlClient())) &&
                !tableImplementor.isInverse() &&
                !joinProp.isMiddleTableDefinition() &&
                table.getParent() != null) {
            ColumnDefinition definition = joinProp.getStorage(strategy);
            analyzeColumns(table.getParent().getAlias(), definition, true, mapper);
            return;
        }
        ColumnDefinition definition = prop.getStorage(strategy);
        String alias = table.getFinalAlias(prop, rawId, ctx.getSqlClient());
        analyzeColumns(alias, definition, false, mapper);
    }

    private static void analyzeColumns(
            String alias,
            ColumnDefinition definition,
            boolean foreignKeyInBaseQuery,
            BaseSelectionMapper mapper
    ) {
        int size = definition.size();
        for (int i = 0; i < size; i++) {
            mapper.columnIndex(alias, definition.name(i), foreignKeyInBaseQuery);
        }
    }
}
