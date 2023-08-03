package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

class RecursiveDtoProp<T extends BaseType, P extends BaseProp> implements DtoProp<T, P> {

    private final P baseProp;

    private final String alias;

    private final DtoType<T, P> targetType;

    RecursiveDtoProp(P baseProp, String alias, DtoType<T, P> selfType) {
        this.baseProp = baseProp;
        this.alias = alias;
        this.targetType = selfType;
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
        return true;
    }

    @Override
    public boolean isIdOnly() {
        return false;
    }

    @Nullable
    @Override
    public String getAlias() {
        return alias;
    }

    @Nullable
    @Override
    public DtoType<T, P> getTargetType() {
        return targetType;
    }

    @Override
    public boolean isRecursive() {
        return true;
    }

    @Override
    public boolean isNewTarget() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("@optional ").append(baseProp.getName());
        if (alias != null) {
            builder.append(" as ").append(alias);
        }
        builder.append(": ...");
        return builder.toString();
    }
}
