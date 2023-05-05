package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public class FetcherSelectionImpl<T> implements FetcherSelection<T>, Ast {

    private final Table<T> table;

    private final Fetcher<?> fetcher;

    public FetcherSelectionImpl(Table<T> table, Fetcher<T> fetcher) {
        this.table = table;
        this.fetcher = fetcher;
    }

    public Table<T> getTable() {
        return table;
    }

    @Override
    public Fetcher<?> getFetcher() {
        return fetcher;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (prop.isColumnDefinition() || prop.getSqlTemplate() instanceof FormulaTemplate) {
                visitor.visitTableReference(TableProxies.resolve(table, visitor.getAstContext()), prop);
            }
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        String separator = "";
        DatabaseMetadata metadata = builder.getAstContext().getSqlClient().getDatabaseMetadata();
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            String alias = TableProxies.resolve(table, builder.getAstContext()).getAlias();
            Storage storage = metadata.getStorage(prop);
            SqlTemplate template = prop.getSqlTemplate();
            if (storage instanceof ColumnDefinition) {
                builder.sql(separator);
                separator = ", ";
                builder.sql(alias, (ColumnDefinition) storage);
            } else if (template instanceof FormulaTemplate) {
                builder.sql(separator);
                separator = ", ";
                builder.sql(((FormulaTemplate)template).toSql(alias));
            }
        }
    }

    @Override
    public String toString() {
        return "FetcherSelectionImpl{" +
                "fetcher=" + fetcher +
                '}';
    }
}
