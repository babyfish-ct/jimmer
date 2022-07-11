package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;

import java.util.Objects;

public class TypedId {

    private final ImmutableType type;

    private final Object id;

    public TypedId(ImmutableType type, Object id) {
        this.type = type;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypedId typedId = (TypedId) o;
        return type.equals(typedId.type) && id.equals(typedId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    public String toString() {
        return "TypedId{" +
                "type=" + type +
                ", id=" + id +
                '}';
    }
}
