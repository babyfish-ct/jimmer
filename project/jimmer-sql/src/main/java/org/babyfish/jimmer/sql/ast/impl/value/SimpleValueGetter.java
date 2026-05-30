package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.table.TableUtils;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class SimpleValueGetter extends AbstractValueGetter {

    private final Table<?> table;

    private final boolean rawId;
    
    private final String columnName;

    private final boolean foreignKey;

    SimpleValueGetter(
            JSqlClientImplementor sqlClientImplementor,
            ImmutableProp valueProp,
            Table<?> table,
            boolean rawId,
            String columnName,
            boolean foreignKey
    ) {
        super(sqlClientImplementor, valueProp);
        this.table = table;
        this.rawId = rawId;
        this.columnName = Objects.requireNonNull(columnName, "The column name cannot be null");
        this.foreignKey = foreignKey;
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
    public boolean isForeignKey() {
        return foreignKey;
    }

    @Override
    public boolean isNullable() {
        return valueProp.isNullable();
    }

    @Override
    public void renderTo(AbstractSqlBuilder<?> builder) {
        if (table != null && builder instanceof SqlBuilder) {
            SqlBuilder sqlBuilder = (SqlBuilder) builder;
            AstContext astContext = sqlBuilder.getAstContext();
            TableImplementor<?> tableImplementor = TableProxies.resolve(table, astContext);
            if (valueProp.isId() && (rawId || TableUtils.isRawIdAllowed(tableImplementor, builder.sqlClient()))) {
                String middleTableAlias = middleTableAlias(sqlBuilder, realTable(sqlBuilder, tableImplementor));
                if (middleTableAlias != null) {
                    builder.sql(middleTableAlias);
                } else {
                    TableImplementor<?> parent = tableImplementor.getParent();
                    builder.sql(alias(sqlBuilder, realTable(sqlBuilder, parent)));
                }
            } else {
                RealTable realTable = realTable(sqlBuilder, tableImplementor);
                builder.sql(alias(sqlBuilder, realTable));
            }
            builder.sql(".");
        }
        builder.sql(columnName);
    }
}
