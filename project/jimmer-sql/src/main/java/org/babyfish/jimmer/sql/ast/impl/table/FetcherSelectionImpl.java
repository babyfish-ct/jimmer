package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public class FetcherSelectionImpl<E> implements FetcherSelection<E>, Ast {

    private final Table<E> table;

    private final Fetcher<E> fetcher;

    public FetcherSelectionImpl(Table<E> table, Fetcher<E> fetcher) {
        this.table = table;
        this.fetcher = fetcher;
    }

    @Override
    public Fetcher<E> getFetcher() {
        return fetcher;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (prop.getStorage() instanceof ColumnDefinition) {
                visitor.visitTableReference(TableProxies.resolve(table, visitor.getAstContext()), prop);
            }
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        String separator = "";
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            String alias = TableProxies.resolve(table, builder.getAstContext()).getAlias();
            Storage storage = prop.getStorage();
            if (storage instanceof ColumnDefinition) {
                builder.sql(separator);
                separator = ", ";
                builder.sql(alias, (ColumnDefinition) storage);
            }
        }
    }

    @Override
    public String toString() {
        return "TableFetcherSelection{" +
                "fetcher=" + fetcher +
                '}';
    }
}
