package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DtoPolymorphicBranch<T extends BaseType, P extends BaseProp> {

    public enum Kind {
        DEFAULT,
        SUBTYPE
    }

    private final Kind kind;

    @Nullable
    private final T targetType;

    @Nullable
    private final String declaredClassName;

    private final DtoType<T, P> dtoType;

    private final boolean implicit;

    private final int line;

    private final int col;

    DtoPolymorphicBranch(
            Kind kind,
            @Nullable T targetType,
            @Nullable String declaredClassName,
            DtoType<T, P> dtoType,
            boolean implicit,
            int line,
            int col
    ) {
        this.kind = kind;
        this.targetType = targetType;
        this.declaredClassName = declaredClassName;
        this.dtoType = dtoType;
        this.implicit = implicit;
        this.line = line;
        this.col = col;
    }

    public Kind getKind() {
        return kind;
    }

    @Nullable
    public T getTargetType() {
        return targetType;
    }

    @Nullable
    public String getDeclaredClassName() {
        return declaredClassName;
    }

    @NotNull
    public String getClassName() {
        if (declaredClassName != null) {
            return declaredClassName;
        }
        return kind == Kind.DEFAULT ? "Default" : targetType.getName();
    }

    public DtoType<T, P> getDtoType() {
        return dtoType;
    }

    public boolean isImplicit() {
        return implicit;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (implicit) {
            builder.append("@implicit ");
        }
        if (kind == Kind.DEFAULT) {
            builder.append("#default");
        } else {
            builder.append(targetType.getQualifiedName());
        }
        if (declaredClassName != null) {
            builder.append(" class ").append(declaredClassName);
        }
        if (dtoType.getDoc() != null) {
            builder.append(" @doc(").append(dtoType.getDoc().replace("\n", "\\n")).append(')');
        }
        for (Anno anno : dtoType.getAnnotations()) {
            builder.append(' ').append(anno);
        }
        if (!dtoType.getSuperInterfaces().isEmpty()) {
            String separator = " implements ";
            for (TypeRef superInterface : dtoType.getSuperInterfaces()) {
                builder.append(separator).append(superInterface);
                separator = ", ";
            }
        }
        builder.append(' ').append(dtoType.bodyToString());
        return builder.toString();
    }
}
