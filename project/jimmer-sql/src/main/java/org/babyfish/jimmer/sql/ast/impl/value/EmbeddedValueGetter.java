package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
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

import java.util.List;
import java.util.Objects;

class EmbeddedValueGetter extends AbstractValueGetter {

    private final Table<?> table;

    private final boolean rawId;

    private final String columnName;

    private final List<ImmutableProp> props;

    private final int hash;

    EmbeddedValueGetter(
            Table<?> table,
            boolean rawId,
            String columnName,
            List<ImmutableProp> props,
            ScalarProvider<Object, Object> scalarProvider,
            String sqlTypeName
    ) {
        super(scalarProvider, sqlTypeName);
        this.table = table;
        this.rawId = rawId;
        this.columnName = Objects.requireNonNull(columnName, "The column name cannot be null");
        this.props = props;
        this.hash = columnName.hashCode() * 31 + props.hashCode();
    }

    @Override
    protected Object getRaw(Object row) {
        for (ImmutableProp prop : props) {
            if (row == null) {
                return null;
            }
            row = ((ImmutableSpi) row).__get(prop.getId());
        }
        return row;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EmbeddedValueGetter)) {
            return false;
        }
        EmbeddedValueGetter other = (EmbeddedValueGetter) obj;
        return hash == other.hash &&
               columnName.equals(other.columnName) &&
               props.equals(other.props);
    }

    @Override
    public String toString() {
        if (props.isEmpty()) {
            return columnName;
        }
        StringBuilder builder = new StringBuilder();
        boolean addDot = false;
        for (ImmutableProp prop : props) {
            if (addDot) {
                builder.append('.');
            } else {
                addDot = true;
            }
            builder.append(prop.getName());
        }
        return builder.toString();
    }

    @Override
    public final ImmutableProp getValueProp() {
        return props.isEmpty() ? null : props.get(props.size() - 1);
    }

    @Override
    public @Nullable String getColumnName() {
        return columnName;
    }

    @Override
    public boolean isNullable() {
        for (ImmutableProp prop : props) {
            if (prop.isNullable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void renderTo(AbstractSqlBuilder<?> builder) {
        if (table != null && builder instanceof SqlBuilder) {
            AstContext astContext = ((SqlBuilder)builder).getAstContext();
            TableImplementor<?> tableImplementor = TableProxies.resolve(table, astContext);
            if (rawId || TableUtils.isRawIdAllowed(tableImplementor, builder.sqlClient())) {
                String middleTableAlias = tableImplementor.getMiddleTableAlias();
                if (middleTableAlias != null) {
                    builder.sql(middleTableAlias);
                } else {
                    builder.sql(tableImplementor.getParent().getAlias());
                }
            } else {
                builder.sql(tableImplementor.getAlias());
            }
            builder.sql(".");
        }
        builder.sql(columnName);
    }
}
