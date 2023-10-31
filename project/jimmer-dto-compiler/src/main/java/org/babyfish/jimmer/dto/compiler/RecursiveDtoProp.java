package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class RecursiveDtoProp<T extends BaseType, P extends BaseProp> implements DtoProp<T, P> {

    private final P baseProp;

    private final String alias;

    private final DtoType<T, P> targetType;

    RecursiveDtoProp(P baseProp, String alias, DtoType<T, P> selfType) {
        this.baseProp = Objects.requireNonNull(baseProp, "baseProp cannot be null");
        this.alias = alias;
        this.targetType = selfType;
    }

    @Override
    public P getBaseProp() {
        return baseProp;
    }

    @Override
    public Map<String, P> getBasePropMap() {
        return Collections.singletonMap(baseProp.getName(), baseProp);
    }

    @Override
    public String getBasePath() {
        return baseProp.getName();
    }

    @Override
    public int getBaseLine() {
        return 0;
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

    @Override
    public boolean isFlat() {
        return false;
    }

    @Override
    public boolean isUnmapped() {
        return false;
    }

    @Nullable
    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public int getAliasLine() {
        return 0;
    }

    @Override
    public @Nullable String getFuncName() {
        return null;
    }

    @Nullable
    @Override
    public DtoType<T, P> getTargetType() {
        return targetType;
    }

    @Override
    public @Nullable EnumType getEnumType() {
        return null;
    }

    @Override
    public boolean isRecursive() {
        return true;
    }

    @Override
    public Mandatory getMandatory() {
        return Mandatory.OPTIONAL;
    }

    @Override
    public boolean isNewTarget() {
        return false;
    }

    @Override
    public Set<LikeOption> getLikeOptions() {
        return Collections.emptySet();
    }

    @Override
    public @Nullable DtoProp<T, P> getNextProp() {
        return null;
    }

    @Override
    public DtoProp<T, P> toTailProp() {
        return this;
    }

    @Override
    public List<Anno> getAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("@optional ").append(baseProp.getName());
        if (alias != null && !alias.equals(getBaseProp().getName())) {
            builder.append(" as ").append(alias);
        }
        builder.append(": ...");
        return builder.toString();
    }
}
