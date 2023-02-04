package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

class DtoPropImpl<T extends BaseType, P extends BaseProp> implements DtoProp<T, P> {

    private final P baseProp;

    @Nullable
    private final String alias;

    @Nullable
    private final DtoType<T, P> targetType;

    private final boolean optional;

    private final boolean idOnly;

    private final boolean recursive;

    DtoPropImpl(
            P baseProp,
            @Nullable String alias,
            @Nullable DtoType<T, P> targetType,
            boolean optional,
            boolean idOnly,
            boolean recursive
    ) {
        this.baseProp = baseProp;
        this.alias = alias;
        this.targetType = targetType;
        this.optional = optional;
        this.idOnly = idOnly;
        this.recursive = recursive;
    }

    @Override
    public P getBaseProp() {
        return baseProp;
    }

    @Override
    public String getName() {
        return alias != null ? alias : baseProp.getName();
    }

    @Override
    public boolean isNullable() {
        return optional || baseProp.isNullable();
    }

    @Override
    public boolean isIdOnly() {
        return idOnly;
    }

    @Override
    @Nullable
    public String getAlias() {
        return alias;
    }

    @Override
    @Nullable
    public DtoType<T, P> getTargetType() {
        return targetType;
    }

    @Override
    public boolean isRecursive() {
        return recursive;
    }

    @Override
    public boolean isNewTarget() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (optional) {
            builder.append("@optional ");
        }
        if (idOnly) {
            builder.append("id(").append(baseProp.getName()).append(')');
        } else {
            builder.append(baseProp.getName());
        }
        if (alias != null) {
            builder.append(" as ").append(alias);
        }
        if (targetType != null) {
            builder.append(": ");
            builder.append(targetType);
        }
        return builder.toString();
    }
}
