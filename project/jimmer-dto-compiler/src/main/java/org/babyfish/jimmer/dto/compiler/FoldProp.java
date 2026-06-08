package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FoldProp<T extends BaseType, P extends BaseProp> implements AbstractProp {

    private final String name;

    private final int line;

    private final int col;

    private final boolean nullable;

    private final List<Anno> annotations;

    @Nullable
    private final String doc;

    private final DtoType<T, P> targetType;

    FoldProp(
            String name,
            int line,
            int col,
            boolean nullable,
            List<Anno> annotations,
            @Nullable String doc,
            DtoType<T, P> targetType
    ) {
        this.name = name;
        this.line = line;
        this.col = col;
        this.nullable = nullable;
        this.annotations = annotations;
        this.doc = doc;
        this.targetType = targetType;
    }

    FoldProp(
            FoldProp<T, P> original,
            String name,
            boolean nullable,
            DtoType<T, P> targetType
    ) {
        this.name = name;
        this.line = original.line;
        this.col = original.col;
        this.nullable = nullable;
        this.annotations = original.annotations;
        this.doc = original.doc;
        this.targetType = targetType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlias() {
        return name;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    public int getAliasLine() {
        return line;
    }

    @Override
    public int getAliasColumn() {
        return col;
    }

    @Override
    public List<Anno> getAnnotations() {
        return annotations;
    }

    @Nullable
    @Override
    public String getDoc() {
        return doc;
    }

    public DtoType<T, P> getTargetType() {
        return targetType;
    }

    @Override
    public String toString() {
        return (nullable ? "@optional " : "") +
                "fold(" +
                name +
                "): " +
                targetType;
    }
}
