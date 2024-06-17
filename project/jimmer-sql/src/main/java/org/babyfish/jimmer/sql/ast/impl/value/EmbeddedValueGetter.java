package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.List;

class EmbeddedValueGetter extends AbstractValueGetter {

    private final String columnName;

    private final List<ImmutableProp> props;

    private final int hash;

    EmbeddedValueGetter(
            String columnName,
            List<ImmutableProp> props,
            ScalarProvider<Object, Object> scalarProvider
    ) {
        super(scalarProvider);
        this.columnName = columnName;
        this.props = props;
        this.hash = columnName.hashCode() * 31 + props.hashCode();
    }

    @Override
    public String columnName() {
        return columnName;
    }

    @Override
    protected Object scalar(Object row) {
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
}
