package org.babyfish.jimmer.meta.impl.dto.ast;

import org.babyfish.jimmer.meta.impl.dto.ast.spi.BaseProp;
import org.babyfish.jimmer.meta.impl.dto.ast.spi.BaseType;
import org.jetbrains.annotations.Nullable;

public class DtoProp<T extends BaseType, P extends BaseProp> {

    private final P baseProp;

    private final boolean negative;

    @Nullable
    private final String alias;

    @Nullable
    private final DtoType<T, P> targetType;

    private final boolean optional;

    private final boolean idOnly;

    private final boolean recursive;

    DtoProp(
            P baseProp,
            @Nullable String alias,
            @Nullable DtoType<T, P> targetType,
            boolean optional,
            boolean idOnly,
            boolean recursive
    ) {
        this.baseProp = baseProp;
        this.alias = alias;
        this.negative = false;
        this.targetType = targetType;
        this.optional = optional;
        this.idOnly = idOnly;
        this.recursive = recursive;
    }

    DtoProp(P baseProp, boolean negative) {
        this.baseProp = baseProp;
        this.negative = negative;
        this.targetType = null;
        this.alias = null;
        this.optional = false;
        this.idOnly = false;
        this.recursive = false;
    }

    public boolean isNegative() {
        return negative;
    }

    public P getBaseProp() {
        return baseProp;
    }

    public boolean isIdOnly() {
        return idOnly;
    }

    @Nullable
    public String getAlias() {
        return alias;
    }

    @Nullable
    public DtoType<T, P> getTargetType() {
        return targetType;
    }

    public boolean isRecursive() {
        return recursive;
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
