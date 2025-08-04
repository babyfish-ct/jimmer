package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class SimpleValueGetter extends AbstractValueGetter {

    private final Table<?> table;

    private final boolean rawId;
    
    private final String columnName;

    SimpleValueGetter(
            JSqlClientImplementor sqlClientImplementor,
            ImmutableProp valueProp,
            Table<?> table,
            boolean rawId,
            String columnName
    ) {
        super(sqlClientImplementor, valueProp);
        this.table = table;
        this.rawId = rawId;
        this.columnName = Objects.requireNonNull(columnName, "The column name cannot be null");
    }

    @Override
    protected Object getRaw(Object value) {
        return value;
    }

    @Override
    public int hashCode() {
        return valueProp.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleValueGetter)) {
            return false;
        }
        SimpleValueGetter other = (SimpleValueGetter) obj;
        return valueProp.equals(other.valueProp);
    }

    @Override
    public String toString() {
        return valueProp.toString();
    }

    @Override
    public final ImmutableProp getValueProp() {
        return valueProp;
    }

    @Override
    public @Nullable String getColumnName() {
        return columnName;
    }

    @Override
    public boolean isNullable() {
        return valueProp.isNullable();
    }

    @Override
    public void renderTo(AbstractSqlBuilder<?> builder) {
        if (table != null && builder instanceof SqlBuilder) {
            AstContext astContext = ((SqlBuilder)builder).getAstContext();
            TableImplementor<?> tableImplementor = TableProxies.resolve(table, astContext);
            if (valueProp.isId() && (rawId || TableUtils.isRawIdAllowed(tableImplementor, builder.sqlClient()))) {
                String middleTableAlias = tableImplementor.realTable(astContext).getMiddleTableAlias();
                if (middleTableAlias != null) {
                    builder.sql(middleTableAlias);
                } else {
                    builder.sql(tableImplementor.getParent().realTable(astContext).getAlias());
                }
            } else {
                builder.sql(tableImplementor.realTable(astContext).getAlias());
            }
            builder.sql(".");
        }
        builder.sql(columnName);
    }
}
