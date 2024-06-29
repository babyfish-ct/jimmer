package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetchPath;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
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
        ImmutableProp embeddedRawReferenceProp = getEmbeddedRawReferenceProp(visitor.getAstContext().getSqlClient());
        if (embeddedRawReferenceProp != null) {
            visitor.visitTableReference(
                    TableProxies.resolve(TableUtils.parent(table), visitor.getAstContext()),
                    embeddedRawReferenceProp,
                    true
            );
            return;
        }
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (prop.isColumnDefinition() || prop.getSqlTemplate() instanceof FormulaTemplate) {
                visitor.visitTableReference(TableProxies.resolve(table, visitor.getAstContext()), prop, field.isRawId());
            }
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
        ImmutableProp embeddedRawReferenceProp = getEmbeddedRawReferenceProp(builder.sqlClient());
        for (Field field : fetcher.getFieldMap().values()) {
            if (field.getProp().isFormula() && field.getProp().getSqlTemplate() == null) {
                continue;
            }
            ImmutableProp prop = field.getProp();
            String alias = TableProxies.resolve(table, builder.getAstContext()).getAlias();
            if (embeddedPropExpression != null) {
                String path = ((PropExpressionImplementor<?>) embeddedPropExpression).getPath();
                renderEmbedded(
                        embeddedRawReferenceProp,
                        embeddedRawReferenceProp != null ?
                            embeddedRawReferenceProp.getTargetType().getIdProp().getStorage(strategy) :
                            ((PropExpressionImplementor<?>) embeddedPropExpression).getProp().getStorage(strategy),
                        field.getChildFetcher(),
                        path != null ? path + '.' + field.getProp().getName() : field.getProp().getName(),
                        builder
                );
            } else {
                Storage storage = prop.getStorage(strategy);
                SqlTemplate template = prop.getSqlTemplate();
                if (storage instanceof EmbeddedColumns) {
                    renderEmbedded(null, (EmbeddedColumns) storage, field.getChildFetcher(), "", builder);
                } else if (storage instanceof ColumnDefinition) {
                    builder.separator().definition(alias, (ColumnDefinition) storage);
                } else if (template instanceof FormulaTemplate) {
                    builder.separator().sql(((FormulaTemplate) template).toSql(alias));
                }
            }
        }
    }

    private void renderEmbedded(
            ImmutableProp embeddedRawReferenceProp,
            EmbeddedColumns columns,
            Fetcher<?> childFetcher,
            String path,
            @NotNull SqlBuilder builder
    ) {
        String alias;
        if (embeddedRawReferenceProp != null) {
            Table<?> parent;
            if (table instanceof TableProxy<?>) {
                TableProxy<?> tableProxy = (TableProxy<?>) table;
                parent = tableProxy.__parent();
            } else {
                TableImplementor<?> tableImplementor = (TableImplementor<?>) table;
                parent = tableImplementor.getParent();
            }
            alias = TableProxies.resolve(parent, builder.getAstContext()).getAlias();
        } else {
            alias = TableProxies.resolve(table, builder.getAstContext()).getAlias();
        }
        MultipleJoinColumns joinColumns =
                embeddedRawReferenceProp != null ?
                        embeddedRawReferenceProp.getStorage(builder.sqlClient().getMetadataStrategy()) :
                        null;
        if (childFetcher == null) {
            for (String columnName : columns.partial(path)) {
                builder.separator().sql(alias).sql(".");
                if (joinColumns != null) {
                    builder.sql(joinColumns.name(joinColumns.referencedIndex(columnName)));
                } else {
                    builder.sql(columnName);
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
                        builder
                );
            }
        }
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
