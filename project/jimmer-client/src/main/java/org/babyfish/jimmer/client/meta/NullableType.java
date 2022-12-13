package org.babyfish.jimmer.client.meta;

public interface NullableType extends Type {

    Type getTargetType();

    static Type unwrap(Type type) {
        if (type instanceof NullableType) {
            return ((NullableType)type).getTargetType();
        }
        return type;
    }
}
