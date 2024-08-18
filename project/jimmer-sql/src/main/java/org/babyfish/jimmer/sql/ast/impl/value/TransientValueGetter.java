package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class TransientValueGetter extends AbstractValueGetter {

    private final List<ImmutableProp> props;

    private final int hash;

    TransientValueGetter(JSqlClientImplementor sqlClient, List<ImmutableProp> props) {
        super(sqlClient, props.get(props.size() - 1));
        this.props = props;
        this.hash = props.hashCode();
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
        if (!(obj instanceof TransientValueGetter)) {
            return false;
        }
        TransientValueGetter other = (TransientValueGetter) obj;
        return hash == other.hash && props.equals(other.props);
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

    @Override
    public final ImmutableProp getValueProp() {
        return props.get(props.size() - 1);
    }

    @Override
    public @Nullable String getColumnName() {
        return null;
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
        throw new IllegalStateException(
                "TransientValueGetter cannot be rendered"
        );
    }
}
