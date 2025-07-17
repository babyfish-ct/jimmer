package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class AbstractConfigurableTypedQueryImpl implements TypedQueryImplementor {

    private final TypedQueryData data;

    private AbstractMutableQueryImpl mutableQuery;

    public AbstractConfigurableTypedQueryImpl(
            TypedQueryData data,
            AbstractMutableQueryImpl mutableQuery
    ) {
        this.data = data;
        this.mutableQuery = mutableQuery;
    }

    public AbstractMutableQueryImpl getMutableQuery() {
        return mutableQuery;
    }

    public TypedQueryData getData() {
        return data;
    }

    @Override
    public List<Selection<?>> getSelections() {
        return data.selections;
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return mutableQuery.getSqlClient();
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        AstContext astContext = visitor.getAstContext();
        astContext.pushStatement(getMutableQuery());
        try {
            Selection<?> idOnlySelection = idOnlyPropExprByOffset();
            if (idOnlySelection != null) {
                mutableQuery.accept(visitor, Collections.singletonList(idOnlySelection), false);
            } else {
                mutableQuery.accept(visitor, data.oldSelections, data.withoutSortingAndPaging);
                for (Selection<?> selection : data.selections) {
                    Ast.from(selection, visitor.getAstContext()).accept(visitor);
                }
                visitBaseTable(mutableQuery.getTableLikeImplementor(), visitor);
            }
        } finally {
            astContext.popStatement();
        }
    }

    @SuppressWarnings("unchecked")
    private void visitBaseTable(TableLikeImplementor<?> tableLikeImplementor, AstVisitor visitor) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            RealTable realBaseTable =
                    tableLikeImplementor.realTable(visitor.getAstContext());
            visitBaseTableImpl(realBaseTable, visitor);
        } else {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) tableLikeImplementor;
            if (tableImplementor.hasBaseTable()) {
                Iterable<TableLikeImplementor<?>> children =
                        (Iterable<TableLikeImplementor<?>>) tableImplementor;
                for (TableLikeImplementor<?> child : children) {
                    visitBaseTable(child, visitor);
                }
            }
        }
    }

    private void visitBaseTableImpl(RealTable realBaseTable, AstVisitor visitor) {
        realBaseTable.getTableLikeImplementor().accept(visitor);
        for (RealTable childBaseTable : realBaseTable) {
            visitBaseTableImpl(childBaseTable, visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> abstractBuilder) {
        renderTo(abstractBuilder, null);
    }

    protected final void renderTo(
            @NotNull AbstractSqlBuilder<?> abstractBuilder,
            @Nullable BaseSelectionAliasRender render
    ) {
        SqlBuilder builder = abstractBuilder.assertSimple();
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(getMutableQuery());
        try {
            if (data.withoutSortingAndPaging || (data.offset == 0 && data.limit == Integer.MAX_VALUE)) {
                renderWithoutPaging(builder, null, render);
            } else {
                PropExpressionImplementor<?> idPropExpr = idOnlyPropExprByOffset();
                if (idPropExpr != null) {
                    renderIdOnlyQuery(idPropExpr, builder);
                } else {
                    SqlBuilder subBuilder = builder.createChildBuilder();
                    renderWithoutPaging(subBuilder, null, render);
                    subBuilder.build(result -> {
                        PaginationContextImpl ctx = new PaginationContextImpl(
                                getMutableQuery().getSqlClient().getSqlFormatter(),
                                data.limit,
                                data.offset,
                                result.get_1(),
                                result.get_2(),
                                result.get_3(),
                                false
                        );
                        mutableQuery.getSqlClient().getDialect().paginate(ctx);
                        return ctx.build();
                    });
                }
            }
            if (data.forUpdate) {
                builder.sql(" for update");
            }
        } finally {
            astContext.popStatement();
        }
    }

    @Override
    public boolean hasVirtualPredicate() {
        return mutableQuery.hasVirtualPredicate();
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        mutableQuery = ctx.resolveVirtualPredicate(mutableQuery);
        return this;
    }

    private void renderWithoutPaging(
            SqlBuilder builder,
            PropExpressionImplementor<?> idPropExpr,
            BaseSelectionAliasRender render
    ) {
        List<RealTable> cteTables = getCteTables(builder.getAstContext());
        if (cteTables.isEmpty()) {
            renderWithoutPagingImpl(builder, idPropExpr, render);
        } else {
            SqlBuilder tmpBuilder = builder.createTempBuilder();
            renderWithoutPagingImpl(tmpBuilder, idPropExpr, render);
            builder.sql("with ").enter(AbstractSqlBuilder.ScopeType.COMMA);
            for (RealTable cteTable : cteTables) {
                builder.separator();
                BaseTableImplementor baseTableImplementor = (BaseTableImplementor) cteTable.getTableLikeImplementor();
                BaseSelectionAliasRender cteRender = builder.getAstContext().getBaseSelectionRender(baseTableImplementor.toSymbol().getQuery());
                assert cteRender != null;
                builder.sql(cteTable.getAlias());
                cteRender.renderCteColumns(cteTable, builder);
                builder.sql(" as ");
                cteTable.renderTo(builder, true);
            }
            builder.leave().sql(" ");
            builder.appendTempBuilder(tmpBuilder);
        }
    }

    private List<RealTable> getCteTables(AstContext ctx) {
        TableLikeImplementor<?> tableLikeImplementor = getMutableQuery().getTableLikeImplementor();
        if (!tableLikeImplementor.hasBaseTable()) {
            return Collections.emptyList();
        }
        RealTable realTable = tableLikeImplementor.realTable(ctx);
        List<RealTable> cteTables = new ArrayList<>();
        collectCteTables(realTable, cteTables);
        return cteTables;
    }

    private void collectCteTables(RealTable realTable, List<RealTable> cteTables) {
        if (realTable.getTableLikeImplementor() instanceof BaseTableImplementor && !(this instanceof TypedBaseQuery<?>)) {
            BaseTableImplementor baseTableImplementor =
                    (BaseTableImplementor) realTable.getTableLikeImplementor();
            if (baseTableImplementor.isCte()) {
                cteTables.add(realTable);
            }
        }
        for (RealTable child : realTable) {
            collectCteTables(child, cteTables);
        }
    }

    private void renderWithoutPagingImpl(
            SqlBuilder builder,
            PropExpressionImplementor<?> idPropExpr,
            BaseSelectionAliasRender render
    ) {
        builder.enter(data.distinct ? SqlBuilder.ScopeType.SELECT_DISTINCT : SqlBuilder.ScopeType.SELECT);
        if (data.hint != null) {
            builder.sql(" ").sql(data.hint).sql(" ");
        }
        if (render != null) {
            List<Selection<?>> selections = data.selections;
            int size = selections.size();
            for (int i = 0; i < size; i++) {
                Selection<?> selection = selections.get(i);
                render.render(i, selection, builder);
            }
        } else if (idPropExpr != null) {
            TableImplementor<?> tableImplementor = TableProxies.resolve(
                    idPropExpr.getTable(),
                    builder.getAstContext()
            );
            tableImplementor.renderSelection(
                    tableImplementor.getImmutableType().getIdProp(),
                    idPropExpr.isRawId(),
                    builder,
                    null,
                    true,
                    OffsetOptimizationWriter::idAlias
            );
        } else {
            renderSelections(builder);
            fakeRenderExportedForeignKeys(mutableQuery.getTableLikeImplementor(),builder);
        }
        builder.leave();
            mutableQuery.renderTo(builder, data.withoutSortingAndPaging, data.reverseSorting);
    }

    private void renderSelections(SqlBuilder builder) {
        for (Selection<?> selection : data.selections) {
            builder.separator();
            if (selection instanceof TableSelection) {
                TableSelection tableSelection = (TableSelection) selection;
                renderAllProps(tableSelection, builder);
            } else if (selection instanceof Table<?>) {
                TableSelection tableSelection = TableProxies.resolve(
                        (Table<?>) selection,
                        builder.getAstContext()
                );
                renderAllProps(tableSelection, builder);
            } else {
                Ast ast = Ast.from(selection, builder.getAstContext());
                if (ast instanceof PropExpressionImplementor<?>) {
                    ((PropExpressionImplementor<?>) ast).renderTo(builder, true);
                } else {
                    ast.renderTo(builder);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fakeRenderExportedForeignKeys(
            TableLikeImplementor<?> tableLikeImplementor,
            SqlBuilder builder
    ) {
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            fakeRenderExportedForeignKeysImpl((BaseTableImplementor) tableLikeImplementor, builder);
        } else {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) tableLikeImplementor;
            if (tableImplementor.hasBaseTable()) {
                Iterable<TableLikeImplementor<?>> children =
                        (Iterable<TableLikeImplementor<?>>) tableImplementor;
                for (TableLikeImplementor<?> child : children) {
                    fakeRenderExportedForeignKeys(child, builder);
                }
            }
        }
    }

    private void fakeRenderExportedForeignKeysImpl(
            BaseTableImplementor baseTableImplementor,
            SqlBuilder builder
    ) {
        AstContext ctx = builder.getAstContext();
        for (Selection<?> selection : baseTableImplementor.toSymbol().getSelections()) {
            if (selection instanceof Table<?>) {
                Table<?> table = (Table<?>) selection;
                TableImplementor<?> tableImplementor = TableProxies.resolve(table, ctx);
                BaseSelectionMapper mapper = ctx.getBaseSelectionMapper(tableImplementor.getBaseTableOwner());
                assert mapper != null;
                RealTable realTable = tableImplementor.realTable(ctx);
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
                    ColumnDefinition definition = prop.getStorage(builder.sqlClient().getMetadataStrategy());
                    int size = definition.size();
                    for (int i = 0; i < size; i++) {
                        // Fake render, only call `columnIndex`, not render it
                        mapper.columnIndex(realTable.getAlias(), definition.name(i));
                    }
                }
            }
        }
    }

    private PropExpressionImplementor<?> idOnlyPropExprByOffset() {
        if (data.offset >= mutableQuery.getSqlClient().getOffsetOptimizingThreshold()) {
            return data.getIdOnlyExpression();
        }
        return null;
    }

    private static void renderAllProps(TableSelection table, SqlBuilder builder) {
        Map<String, ImmutableProp> selectableProps = table
                .getImmutableType()
                .getSelectableProps();
        for (ImmutableProp prop : selectableProps.values()) {
            builder.separator();
            table.renderSelection(prop, true, builder, null);
        }
    }

    private void renderIdOnlyQuery(PropExpressionImplementor<?> idPropExpr, SqlBuilder builder) {
        MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
        OffsetOptimizationWriter writer =
                new OffsetOptimizationWriter(builder, strategy);
        TableImplementor<?> tableImplementor = TableProxies.resolve(
                idPropExpr.getTable(),
                builder.getAstContext()
        );
        builder.enter(SqlBuilder.ScopeType.SELECT);
        if (data.selections.get(0) instanceof FetcherSelection<?>) {
            for (Field field : ((FetcherSelection<?>)data.selections.get(0)).getFetcher().getFieldMap().values()) {
                writer.prop(field.getProp(), OffsetOptimizationWriter.ALIAS, false);
            }
        } else {
            for (ImmutableProp prop : tableImplementor.getImmutableType().getProps().values()) {
                writer.prop(prop, OffsetOptimizationWriter.ALIAS, false);
            }
        }
        builder.leave();
        builder.from().enter(SqlBuilder.ScopeType.SUB_QUERY);
        SqlBuilder subBuilder = builder.createChildBuilder();
        renderWithoutPaging(subBuilder, idPropExpr, null);
        subBuilder.build(result -> {
            PaginationContextImpl ctx = new PaginationContextImpl(
                    getMutableQuery().getSqlClient().getSqlFormatter(),
                    data.limit,
                    data.offset,
                    result.get_1(),
                    result.get_2(),
                    result.get_3(),
                    true
            );
            mutableQuery.getSqlClient().getDialect().paginate(ctx);
            return ctx.build();
        });
        builder
                .leave()
                .sql(" ")
                .sql(OffsetOptimizationWriter.CORE_ALIAS)
                .sql(" inner join ")
                .sql(tableImplementor.getImmutableType().getTableName(strategy))
                .sql(" ")
                .sql(OffsetOptimizationWriter.ALIAS)
                .on();
        writer.prop(
                tableImplementor.getImmutableType().getIdProp(),
                OffsetOptimizationWriter.ALIAS,
                true
        );
        builder.sql(" = ");
        int size = tableImplementor.getImmutableType().getIdProp().<ColumnDefinition>getStorage(strategy).size();
        if (size == 1) {
            builder.sql(OffsetOptimizationWriter.CORE_ALIAS).sql(".");
            builder.sql(OffsetOptimizationWriter.idAlias(0));
        } else {
            builder.sql("(");
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    builder.sql(", ");
                }
                builder.sql(OffsetOptimizationWriter.CORE_ALIAS).sql(".");
                builder.sql(OffsetOptimizationWriter.idAlias(i));
            }
            builder.sql(")");
        }
        if (getMutableQuery().getSqlClient().getDialect().getOffsetOptimizationNumField() != null) {
            builder
                    .enter(SqlBuilder.ScopeType.ORDER_BY)
                    .sql(OffsetOptimizationWriter.CORE_ALIAS)
                    .sql(".")
                    .sql(OffsetOptimizationWriter.ROW_NUMBER_ALIAS)
                    .leave();
        }
    }

    private static class OffsetOptimizationWriter {

        private static final String ALIAS = "optimize_";

        private static final String CORE_ALIAS = "optimize_core_";

        private static final String CORE_ID_ALIAS = "optimize_core_id_";

        private static final String ROW_NUMBER_ALIAS = OracleDialect.OPTIMIZE_CORE_ROW_NUMBER_ALIAS;

        private final SqlBuilder builder;

        private final MetadataStrategy strategy;

        OffsetOptimizationWriter(SqlBuilder builder, MetadataStrategy strategy) {
            this.builder = builder;
            this.strategy = strategy;
        }

        public void prop(ImmutableProp prop, String alias, boolean multiColumnsAsTuple) {
            SqlTemplate template = prop.getSqlTemplate();
            if (template instanceof FormulaTemplate) {
                builder.separator().sql(((FormulaTemplate)template).toSql(alias));
                return;
            }
            Storage storage = prop.getStorage(strategy);
            if (storage instanceof ColumnDefinition) {
                ColumnDefinition definition = (ColumnDefinition) storage;
                int size = definition.size();
                if (size == 1) {
                    builder.separator().sql(alias).sql(".").sql(definition.name(0));
                } else if (multiColumnsAsTuple) {
                    builder.enter(SqlBuilder.ScopeType.TUPLE);
                    for (int i = 0; i < size; i++) {
                        builder.separator().sql(alias).sql(".").sql(definition.name(i));
                    }
                    builder.leave();
                } else {
                    for (int i = 0; i < size; i++) {
                        builder.separator().sql(alias).sql(".").sql(definition.name(i));
                    }
                }
            }
        }

        public static String idAlias(int index) {
            return index == 0 ? CORE_ID_ALIAS : CORE_ID_ALIAS + index + '_';
        }
    }
}
