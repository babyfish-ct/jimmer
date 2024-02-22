package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.runtime.NullableType;
import org.babyfish.jimmer.client.runtime.SimpleType;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.impl.util.Classes;

class Types {

    private Types() {}

    public static String boxedTypeName(Type type) {
        if (type instanceof NullableType) {
            type = ((NullableType) type).getTargetType();
        }
        if (type instanceof SimpleType) {
            Class<?> targetJavaType = ((SimpleType)type).getJavaType();
            if (targetJavaType.isPrimitive()) {
                return Classes.boxTypeOf(targetJavaType).getSimpleName();
            }
        }
        return null;
    }
}
