package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;

import java.util.Objects;

public final class SavePath {

    private final ImmutableType type;

    private final ImmutableProp prop;

    private final SavePath parent;

    private SavePath(ImmutableType type) {
        this.type = Objects.requireNonNull(type, "`type` cannot be null");
        this.prop = null;
        this.parent = null;
    }

    private SavePath(ImmutableProp prop, SavePath parent) {
        if (!prop.isAssociation(TargetLevel.PERSISTENT)) {
            throw new IllegalArgumentException("\"" + prop + "\" is not persistent association property");
        }
        this.type = prop.getTargetType();
        this.prop = prop;
        this.parent = parent;
    }

    public static SavePath root(ImmutableType type) {
        return new SavePath(type);
    }

    public SavePath to(ImmutableProp prop) {
        return new SavePath(prop, this);
    }

    public ImmutableType getType() {
        return type;
    }

    public ImmutableProp getProp() {
        return prop;
    }

    public SavePath getParent() {
        return parent;
    }

    public boolean contains(ImmutableProp prop) {
        if (this.prop == prop) {
            return true;
        }
        if (parent != null) {
            return parent.contains(prop);
        }
        return false;
    }

    public boolean contains(TypedProp.Association<?, ?> prop) {
        return contains(prop.unwrap());
    }

    public boolean contains(ImmutableType type) {
        if (this.type == type) {
            return true;
        }
        if (parent != null) {
            return parent.contains(type);
        }
        return false;
    }

    public boolean contains(Class<?> type) {
        return contains(ImmutableType.get(type));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, prop, parent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavePath other = (SavePath) o;
        return type == other.type &&
                Objects.equals(prop, other.prop) &&
                Objects.equals(parent, other.parent);
    }

    @Override
    public String toString() {
        if (parent == null) {
            return "<root>";
        }
        return parent + "." + prop.getName();
    }
}
