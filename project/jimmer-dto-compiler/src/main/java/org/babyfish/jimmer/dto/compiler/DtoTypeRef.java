package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

public final class DtoTypeRef<T extends BaseType, P extends BaseProp> implements DtoPropTarget<T, P> {

    private final String qualifiedName;

    private final T targetBaseType;

    private final int line;

    private final int col;

    @Nullable
    private DtoType<T, P> sourceType;

    @Nullable
    private DtoTypeInfo<T> typeInfo;

    DtoTypeRef(String qualifiedName, T baseType, int line, int col) {
        this.qualifiedName = qualifiedName;
        this.targetBaseType = baseType;
        this.line = line;
        this.col = col;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public T getTargetBaseType() {
        return targetBaseType;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return col;
    }

    @Nullable
    public DtoType<T, P> getSourceType() {
        return sourceType;
    }

    @Nullable
    public DtoTypeInfo<T> getTypeInfo() {
        return typeInfo;
    }

    void resolve(DtoTypeInfo<T> typeInfo, @Nullable DtoType<T, P> sourceType) {
        this.typeInfo = typeInfo;
        this.sourceType = sourceType;
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
