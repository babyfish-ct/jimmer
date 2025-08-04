package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionMapper;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetchPath;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.fetcher.impl.JoinFetchFieldVisitor;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                    visitor.getAstContext().pushStatement(((ConfigurableBaseQueryImpl<?>)q).getMutableQuery());
                    accept((Table<?>) mergedBy.getSelections().get(index), visitor);
                    visitor.getAstContext().popStatement();
                }
            }
        }
    }

    private void accept(Table<?> table, AstVisitor visitor) {
        ImmutableProp embeddedRawReferenceProp = getEmbeddedRawReferenceProp(visitor.getAstContext().getSqlClient());
        if (embeddedRawReferenceProp != null) {
            visitor.visitTableReference(
                    TableProxies
                            .resolve(TableUtils.parent(table), visitor.getAstContext())
                            .realTable(visitor.getAstContext()),
                    embeddedRawReferenceProp,
                    true
            );
            return;
        }
        RealTable realTable = TableProxies
                .resolve(table, visitor.getAstContext())
                .realTable(visitor.getAstContext());
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (prop.isColumnDefinition() ||
                    prop.getSqlTemplate() instanceof FormulaTemplate ||
                    JoinFetchFieldVisitor.isJoinField(field, visitor.getAstContext().getSqlClient())) {
                visitor.visitTableReference(realTable, prop, field.isRawId());
            }
        }
        visitor.visitTableFetcher(realTable, fetcher);
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
                .realTable(builder.getAstContext());
        new JoinFetchFieldVisitor(builder.sqlClient()) {

            private RealTable table = realTable;

            @Override
            protected Object enter(Field field) {
                RealTable oldTable = table;
                TableLikeImplementor<?> implementor = oldTable.getTableLikeImplementor();
                if (implementor instanceof TableImplementor<?>) {
                    TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
                    this.table = tableImplementor
                            .joinFetchImplementor(field.getProp(), oldTable.getBaseTableOwner())
                            .realTable(ctx);
                }
                return oldTable;
            }

            @Override
            protected void leave(Field field, Object enterValue) {
                this.table = (RealTable) enterValue;
            }

            @Override
            protected void visit(Field field, int depth) {
                if (field.getProp().isFormula() && field.getProp().getSqlTemplate() == null) {
                    return;
                }
                ImmutableProp prop = field.getProp();
                String alias = table.getAlias();
                BaseSelectionMapper mapper =
                        depth == 0 ?
                                builder.getAstContext().getBaseSelectionMapper(table.getBaseTableOwner()) :
                                null;
                if (embeddedPropExpression != null) {
                    String path = ((PropExpressionImplementor<?>) embeddedPropExpression).getPath();
                    renderEmbedded(
                            embeddedRawReferenceProp,
                            embeddedRawReferenceProp != null ?
                                    embeddedRawReferenceProp.getTargetType().getIdProp().getStorage(strategy) :
                                    ((PropExpressionImplementor<?>) embeddedPropExpression).getProp().getStorage(strategy),
                            field.getChildFetcher(),
                            path != null ? path + '.' + field.getProp().getName() : field.getProp().getName(),
                            mapper,
                            builder
                    );
                } else {
                    Storage storage = prop.getStorage(strategy);
                    SqlTemplate template = prop.getSqlTemplate();
                    if (storage instanceof EmbeddedColumns) {
                        renderEmbedded(null, (EmbeddedColumns) storage, field.getChildFetcher(), "", mapper, builder);
                    } else if (storage instanceof ColumnDefinition) {
                        builder.separator().definition(alias, (ColumnDefinition) storage, mapper);
                    } else if (template instanceof FormulaTemplate) {
                        builder.separator();
                        if (mapper != null) {
                            builder.sql(mapper.getAlias())
                                    .sql(".c")
                                    .sql(Integer.toString(mapper.formulaIndex(alias, (FormulaTemplate) template)));
                        } else {
                            builder.sql(((FormulaTemplate) template).toSql(alias));
                        }
                    }
                }
            }

            private void renderEmbedded(
                    ImmutableProp embeddedRawReferenceProp,
                    EmbeddedColumns columns,
                    Fetcher<?> childFetcher,
                    String path,
                    BaseSelectionMapper mapper,
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
                        if (mapper != null) {
                            builder
                                    .sql(mapper.getAlias())
                                    .sql(".c")
                                    .sql(Integer.toString(mapper.columnIndex(realTable.getAlias(), columnName)));
                        } else {
                            builder
                                    .sql(realTable.getAlias()).sql(".")
                                    .sql(columnName);
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
                                mapper,
                                builder
                        );
                    }
                }
            }
        }.visit(fetcher);
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
            joinProp= tableImplementor.getJoinProp();
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
