package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

public final class DtoTypeRef<T extends BaseType, P extends BaseProp> implements DtoPropTarget<T, P> {

    private final String qualifiedName;

    private final T baseType;

    private final int line;

    private final int col;

    @Nullable
    private DtoType<T, P> sourceType;

    DtoTypeRef(String qualifiedName, T baseType, int line, int col) {
        this.qualifiedName = qualifiedName;
        this.baseType = baseType;
        this.line = line;
        this.col = col;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public T getBaseType() {
        return baseType;
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

    void resolve(DtoType<T, P> sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
