package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class FetcherSelectionImpl<T> implements FetcherSelection<T>, Ast {

    private final Table<?> table;

    private final Fetcher<?> fetcher;

    @Nullable
    private final Function<?, T> converter;

    public FetcherSelectionImpl(Table<T> table, Fetcher<T> fetcher) {
        this.table = table;
        this.fetcher = fetcher;
        this.converter = null;
    }

    public FetcherSelectionImpl(Table<?> table, Fetcher<?> fetcher, @Nullable Function<?, T> converter) {
        this.table = table;
        this.fetcher = fetcher;
        this.converter = converter;
    }

    public Table<?> getTable() {
        return table;
    }

    @Override
    public Fetcher<?> getFetcher() {
        return fetcher;
    }

    @Nullable
    @Override
    public Function<?, T> getConverter() {
        return converter;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
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
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            String alias = TableProxies.resolve(table, builder.getAstContext()).getAlias();
            Storage storage = prop.getStorage(strategy);
            SqlTemplate template = prop.getSqlTemplate();
            if (storage instanceof ColumnDefinition) {
                builder.separator().definition(alias, (ColumnDefinition) storage);
            } else if (template instanceof FormulaTemplate) {
                builder.separator().sql(((FormulaTemplate)template).toSql(alias));
            }
        }
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
