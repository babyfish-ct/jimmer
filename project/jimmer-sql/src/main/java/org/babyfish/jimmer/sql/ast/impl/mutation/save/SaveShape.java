package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

class SaveShape {

    private final ImmutableType type;

    private final List<ImmutableProp> props;

    private final int hash;

    private Boolean isIdLoaded;

    private Boolean isNonIdLoaded;

    private Boolean isAllKeysLoaded;

    private SaveShape(ImmutableType type, List<ImmutableProp> props, int hash) {
        this.type = type;
        this.props = props;
        this.hash = hash;
    }

    public static SaveShape of(ImmutableSpi spi) {
        List<ImmutableProp> props = new ArrayList<>();
        int hash = 0;
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (spi.__isLoaded(prop.getId())) {
                props.add(prop);
                hash += prop.hashCode();
            }
        }
        return new SaveShape(spi.__type(), Collections.unmodifiableList(props), hash);
    }

    public ImmutableType getType() {
        return type;
    }

    public List<ImmutableProp> getProps() {
        return props;
    }

    public boolean isIdLoaded() {
        Boolean ref = isIdLoaded;
        if (ref == null) {
            boolean result = false;
            for (ImmutableProp prop : props) {
                if (prop.isId()) {
                    result = true;
                    break;
                }
            }
            this.isIdLoaded = ref = result;
        }
        return ref;
    }

    public boolean isNonIdLoaded() {
        Boolean ref = isNonIdLoaded;
        if (ref == null) {
            boolean result = false;
            for (ImmutableProp prop : props) {
                if (!prop.isId()) {
                    result = true;
                    break;
                }
            }
            this.isNonIdLoaded = ref = result;
        }
        return ref;
    }

    public boolean isAllKeysLoaded() {
        Boolean ref = isAllKeysLoaded;
        if (ref == null) {
            this.isAllKeysLoaded = ref = new HashSet<>(props).containsAll(type.getKeyProps());
        }
        return ref;
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
        if (!(obj instanceof SaveShape)) {
            return false;
        }
        SaveShape other = (SaveShape) obj;
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
