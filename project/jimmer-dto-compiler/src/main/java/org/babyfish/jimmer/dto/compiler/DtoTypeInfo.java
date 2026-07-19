package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseType;

public final class DtoTypeInfo<T extends BaseType> {

    private final T baseType;

    private final DtoTypeKind kind;

    public DtoTypeInfo(T baseType, DtoTypeKind kind) {
        this.baseType = baseType;
        this.kind = kind;
    }

    public T getBaseType() {
        return baseType;
    }

    public DtoTypeKind getKind() {
        return kind;
    }
}
