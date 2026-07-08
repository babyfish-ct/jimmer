package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryRead;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryReadSupport;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetchPath;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.fetcher.impl.JoinFetchFieldVisitor;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FetcherSelectionImpl<T> implements FetcherSelection<T>, Ast {

    private final FetchPath path;

    private final Table<?> table;

    private final PropExpression.Embedded<?> embeddedPropExpression;

    private final Fetcher<?> fetcher;

    @Nullable
    private final Function<?, ?> converter;

    public FetcherSelectionImpl(Table<T> table, Fetcher<T> fetcher) {
        this.path = null;
        this.table = table;
        this.embeddedPropExpression = null;
        this.fetcher = fetcher;
        this.converter = null;
    }

    public FetcherSelectionImpl(Table<T> table, FetchPath path, Fetcher<T> fetcher) {
        this.path = path;
        this.table = table;
        this.embeddedPropExpression = null;
        this.fetcher = fetcher;
        this.converter = null;
    }

    public FetcherSelectionImpl(Table<?> table, Fetcher<?> fetcher, @Nullable Function<?, T> converter) {
        this.path = null;
        this.table = table;
        this.fetcher = fetcher;
        this.embeddedPropExpression = null;
        this.converter = converter;
    }

    public FetcherSelectionImpl(PropExpression.Embedded<T> embeddedPropExpression, Fetcher<T> fetcher) {
        this.path = null;
        this.table = ((PropExpressionImplementor<?>) embeddedPropExpression).getTable();
        this.fetcher = fetcher;
        this.embeddedPropExpression = embeddedPropExpression;
        this.converter = null;
    }

    public FetcherSelectionImpl(
            PropExpression.Embedded<?> embeddedPropExpression,
            Fetcher<?> fetcher,
            @Nullable Function<?, ?> converter
    ) {
        this.path = null;
        this.table = ((PropExpressionImplementor<?>) embeddedPropExpression).getTable();
        this.fetcher = fetcher;
        this.embeddedPropExpression = embeddedPropExpression;
        this.converter = converter;
    }

    public Table<?> getTable() {
        return table;
    }

    @Override
    public FetchPath getPath() {
        return path;
    }

    @Override
    public PropExpression.Embedded<?> getEmbeddedPropExpression() {
        return embeddedPropExpression;
    }

    @Override
    public Fetcher<?> getFetcher() {
        return fetcher;
    }

    @Nullable
    @Override
    public Function<?, ?> getConverter() {
        return converter;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        accept(table, visitor);
        BaseTableOwner baseTableOwner = BaseTableOwner.of(table);
        if (baseTableOwner != null) {
            int index = baseTableOwner.getIndex();
            TypedBaseQueryImplementor<?> query = baseTableOwner.getBaseTable().getQuery();
            MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(query);
            if (mergedBy != null) {
                for (TypedBaseQueryImplementor<?> q : mergedBy.getExpandedQueries()) {
                    visitor.getAstContext().pushStatement(((ConfigurableBaseQueryImpl<?>) q).getMutableQuery());
                    accept((Table<?>) mergedBy.getSelections().get(index), visitor);
                    visitor.getAstContext().popStatement();
                }
            }
        }
    }

    private void accept(Table<?> table, AstVisitor visitor) {
        ImmutableProp embeddedRawReferenceProp = getEmbeddedRawReferenceProp(visitor.getAstContext().getSqlClient());
        if (embeddedRawReferenceProp != null) {
            TableImplementor<?> tableImplementor = TableProxies.resolve(TableUtils.parent(table), visitor.getAstContext());
            visitor.visitTableReference(
                    visitor.realTableForAnalysis(tableImplementor),
                    embeddedRawReferenceProp,
                    true
            );
            return;
        }
        TableImplementor<?> tableImplementor = TableProxies.resolve(table, visitor.getAstContext());
        RealTable realTable = visitor.realTableForAnalysis(tableImplementor);
        if (tableImplementor.getPolymorphicDiscriminatorProp() != null) {
            visitor.visitTableReference(realTable, null, false);
        }
        acceptFields(visitor, realTable, fetcher);
        visitor.visitTableFetcher(realTable, fetcher);
    }

    private void acceptFields(AstVisitor visitor, RealTable realTable, Fetcher<?> fetcher) {
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (prop.isColumnDefinition() ||
                    prop.getSqlTemplate() instanceof FormulaTemplate ||
                    JoinFetchFieldVisitor.isJoinField(field, visitor.getAstContext().getSqlClient())) {
                visitor.visitTableFetcherField(realTable, field);
            }
        }
        TableLikeImplementor<?> implementor = realTable.getTableLikeImplementor();
        if (!(implementor instanceof TableImplementor<?>)) {
            return;
        }
        TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
        for (Map.Entry<ImmutableType, Fetcher<?>> e : typeBranchFetcherMap(fetcher).entrySet()) {
            if (!JoinFetchFieldVisitor.hasTableFields(e.getValue(), visitor.getAstContext().getSqlClient(), true)) {
                continue;
            }
            TableImplementor<?> treatedImplementor = tableImplementor.treatAsImplementor(e.getKey(), JoinType.LEFT);
            RealTable treatedTable = visitor.realTableForAnalysis(treatedImplementor);
            acceptFields(visitor, treatedTable, e.getValue());
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> abstractBuilder) {
        SqlBuilder builder = abstractBuilder.assertSimple();
        AstContext ctx = builder.getAstContext();
        JSqlClientImplementor sqlClient = ctx.getSqlClient();
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        ImmutableProp embeddedRawReferenceProp = getEmbeddedRawReferenceProp(builder.sqlClient());
        RealTable realTable = TableProxies
                .resolve(table, builder.getAstContext())
                .realTable(builder.getQueryRenderContext());
        Set<RealTable> discriminatorRenderedTables = Collections.newSetFromMap(new IdentityHashMap<>());
        JoinFetchFieldVisitor visitor = new JoinFetchFieldVisitor(builder.sqlClient()) {

            private RealTable table = realTable;

            private int typeBranchDepth;

            @Override
            protected Object enter(Field field) {
                RealTable oldTable = table;
                TableLikeImplementor<?> implementor = oldTable.getTableLikeImplementor();
                if (implementor instanceof TableImplementor<?>) {
                    TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
                    this.table = tableImplementor
                            .joinFetchImplementor(field.getProp(), oldTable.getBaseTableOwner())
                            .realTable(builder.getQueryRenderContext());
                }
                return oldTable;
            }

            @Override
            protected void leave(Field field, Object enterValue) {
                this.table = (RealTable) enterValue;
            }

            @Override
            protected boolean shouldVisitTypeBranch(ImmutableType branchType, Fetcher<?> fetcher) {
                return JoinFetchFieldVisitor.hasTableFields(fetcher, builder.sqlClient(), true);
            }

            @Override
            protected Object enterTypeBranch(ImmutableType branchType) {
                RealTable oldTable = table;
                TableLikeImplementor<?> implementor = oldTable.getTableLikeImplementor();
                if (implementor instanceof TableImplementor<?>) {
                    this.table = ((TableImplementor<?>) implementor)
                            .treatAsImplementor(branchType, JoinType.LEFT)
                            .realTable(builder.getQueryRenderContext());
                }
                typeBranchDepth++;
                return oldTable;
            }

            @Override
            protected void leaveTypeBranch(ImmutableType branchType, Object enterValue) {
                typeBranchDepth--;
                this.table = (RealTable) enterValue;
            }

            @Override
            protected void visit(Field field, int depth) {
                if (field.getProp().isFormula() && field.getProp().getSqlTemplate() == null) {
                    return;
                }
                ImmutableProp prop = field.getProp();
                if (typeBranchDepth != 0 && prop.isId()) {
                    return;
                }
                if (prop.isDiscriminator() && (
                        typeBranchDepth != 0 ||
                                isRenderedByDiscriminatorSlot(table, prop)
                )) {
                    return;
                }
                BaseQueryReadSupport readSupport =
                        depth == 0 ?
                                builder.getQueryRenderContext().getBaseQueryReadSupport() :
                                null;
                BaseTableOwner baseTableOwner = readSupport != null ? table.getBaseTableOwner() : null;
                if (embeddedPropExpression != null) {
                    String path = ((PropExpressionImplementor<?>) embeddedPropExpression).getPath();
                    renderEmbedded(
                            embeddedRawReferenceProp,
                            embeddedRawReferenceProp != null ?
                                    embeddedRawReferenceProp.getTargetType().getIdProp().getStorage(strategy) :
                                    ((PropExpressionImplementor<?>) embeddedPropExpression).getProp().getStorage(strategy),
                            field.getChildFetcher(),
                            path != null ? path + '.' + field.getProp().getName() : field.getProp().getName(),
                            readSupport,
                            baseTableOwner,
                            builder
                    );
                } else {
                    Storage storage = prop.getStorage(strategy);
                    SqlTemplate template = prop.getSqlTemplate();
                    if (storage instanceof EmbeddedColumns) {
                        renderEmbedded(
                                null,
                                (EmbeddedColumns) storage,
                                field.getChildFetcher(),
                                "",
                                readSupport,
                                baseTableOwner,
                                builder
                        );
                    } else if (storage instanceof ColumnDefinition) {
                        builder.separator();
                        table.renderSelection(prop, field.isRawId(), builder, null, true, null, false);
                    } else if (template instanceof FormulaTemplate) {
                        builder.separator();
                        BaseQueryRead read = readSupport != null ?
                                readSupport.formula(baseTableOwner, table, (FormulaTemplate) template) :
                                null;
                        if (read != null) {
                            renderBaseQueryRead(builder, read, 0);
                        } else {
                            builder.sql(((FormulaTemplate) template).toSql(builder.alias(table)));
                        }
                    }
                }
                if (typeBranchDepth == 0 && prop.isId()) {
                    renderDiscriminator(table);
                }
            }

            private void renderDiscriminator(RealTable table) {
                ImmutableProp discriminatorProp = discriminatorProp(table);
                if (discriminatorProp == null || !discriminatorRenderedTables.add(table)) {
                    return;
                }
                builder.separator();
                table.renderColumn(
                        builder,
                        ((SingleColumn) discriminatorProp.getStorage(
                                builder.getAstContext().getSqlClient().getMetadataStrategy()
                        )).getName(),
                        false,
                        null,
                        null
                );
            }

            @Nullable
            private ImmutableProp discriminatorProp(RealTable table) {
                TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
                if (!(implementor instanceof TableImplementor<?>)) {
                    return null;
                }
                return ((TableImplementor<?>) implementor).getPolymorphicDiscriminatorProp();
            }

            private boolean isRenderedByDiscriminatorSlot(RealTable table, ImmutableProp prop) {
                ImmutableProp discriminatorProp = discriminatorProp(table);
                return discriminatorProp != null &&
                        prop.isDiscriminator() &&
                        prop.toOriginal() == discriminatorProp.toOriginal();
            }

            private void renderEmbedded(
                    ImmutableProp embeddedRawReferenceProp,
                    EmbeddedColumns columns,
                    Fetcher<?> childFetcher,
                    String path,
                    BaseQueryReadSupport readSupport,
                    BaseTableOwner baseTableOwner,
                    SqlBuilder builder
            ) {
                RealTable realTable;
                if (embeddedRawReferenceProp != null) {
                    realTable = table.getParent();
                } else {
                    realTable = table;
                }
                MultipleJoinColumns joinColumns =
                        embeddedRawReferenceProp != null ?
                                embeddedRawReferenceProp.getStorage(builder.sqlClient().getMetadataStrategy()) :
                                null;
                if (childFetcher == null) {
                    for (String columnName : columns.partial(path)) {
                        if (joinColumns != null) {
                            columnName = joinColumns.name(joinColumns.referencedIndex(columnName));
                        }
                        builder.separator();
                        BaseQueryRead read = readSupport != null ?
                                readSupport.column(baseTableOwner, realTable, columnName, false) :
                                null;
                        if (read != null) {
                            renderBaseQueryRead(builder, read, 0);
                        } else {
                            realTable.renderColumn(builder, columnName, false, null, null);
                        }
                    }
                } else {
                    for (Field field : childFetcher.getFieldMap().values()) {
                        ImmutableProp prop = field.getProp();
                        if (prop.isFormula() && prop.getSqlTemplate() == null) {
                            continue;
                        }
                        String propName = field.getProp().getName();
                        renderEmbedded(
                                embeddedRawReferenceProp,
                                columns,
                                field.getChildFetcher(),
                                path.isEmpty() ? propName : path + '.' + propName,
                                readSupport,
                                baseTableOwner,
                                builder
                        );
                    }
                }
            }

            private void renderBaseQueryRead(SqlBuilder builder, BaseQueryRead read, int index) {
                builder
                        .sql(builder.alias(read.getRealBaseTable()))
                        .sql(".c")
                        .sql(Integer.toString(read.index(index)));
            }
        };
        visitor.visit(fetcher);
    }

    private static Map<ImmutableType, Fetcher<?>> typeBranchFetcherMap(Fetcher<?> fetcher) {
        if (fetcher instanceof FetcherImplementor<?>) {
            return ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap();
        }
        return Collections.emptyMap();
    }

    private ImmutableProp getEmbeddedRawReferenceProp(JSqlClientImplementor sqlClient) {
        if (embeddedPropExpression == null) {
            return null;
        }
        PropExpressionImpl<?> pei = (PropExpressionImpl<?>) embeddedPropExpression;
        if (!pei.getProp().isId()) {
            return null;
        }
        ImmutableProp joinProp;
        if (table instanceof TableProxy<?>) {
            TableProxy<?> tableProxy = (TableProxy<?>) table;
            joinProp = tableProxy.__prop();
            if (tableProxy.__isInverse()) {
                joinProp = joinProp.getMappedBy();
            }
        } else {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) table;
            joinProp = tableImplementor.getJoinProp();
            if (tableImplementor.isInverse()) {
                joinProp = joinProp.getMappedBy();
            }
        }
        if (joinProp == null || !joinProp.isColumnDefinition()) {
            return null;
        }
        if (sqlClient.getFilters().getTargetFilter(joinProp) != null) {
            return null;
        }
        return joinProp;
    }

    @Override
    public boolean hasVirtualPredicate() {
        return false;
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        return this;
    }

    @Override
    public String toString() {
        return "FetcherSelectionImpl{" +
                "fetcher=" + fetcher +
                '}';
    }
}
