package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SavedShape {

    private final List<ImmutableProp> props;

    private final int hash;

    private SavedShape(List<ImmutableProp> props, int hash) {
        this.props = props;
        this.hash = hash;
    }

    public static SavedShape of(ImmutableSpi spi) {
        List<ImmutableProp> props = new ArrayList<>();
        int hash = 0;
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (spi.__isLoaded(prop.getId())) {
                props.add(prop);
                hash += prop.hashCode();
            }
        }
        return new SavedShape(Collections.unmodifiableList(props), hash);
    }

    public List<ImmutableProp> getProps() {
        return props;
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
        if (!(obj instanceof SavedShape)) {
            return false;
        }
        SavedShape other = (SavedShape) obj;
        return hash == other.hash && props.equals(other.props);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean addComma = false;
        for (ImmutableProp prop : props) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(prop.getName());
        }
        builder.append(']');
        return builder.toString();
    }
}
