package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

class DtoPropImpl<T extends BaseType, P extends BaseProp> implements DtoProp<T, P> {

    private final P baseProp;

    private final int baseLine;

    @Nullable
    private final String alias;

    private final int aliasLine;

    private final DtoType<T, P> targetType;

    private final boolean optional;

    private final String funcName;

    private final boolean recursive;

    DtoPropImpl(
            P baseProp,
            int baseLine,
            @Nullable String alias,
            int aliasLine,
            @Nullable DtoType<T, P> targetType,
            boolean optional,
            String funcName,
            boolean recursive
    ) {
        this.baseProp = baseProp;
        this.baseLine = baseLine;
        this.alias = alias;
        this.aliasLine = aliasLine;
        this.targetType = targetType;
        this.optional = optional;
        this.funcName = funcName;
        this.recursive = recursive;
    }

    @Override
    public P getBaseProp() {
        return baseProp;
    }

    @Override
    public int getBaseLine() {
        return baseLine;
    }

    @Override
    public String getName() {
        return alias != null ? alias : baseProp.getName();
    }

    @Override
    public int getAliasLine() {
        return aliasLine;
    }

    @Override
    public boolean isNullable() {
        return optional || baseProp.isNullable();
    }

    @Override
    public boolean isIdOnly() {
        return "id".equals(funcName);
    }

    @Override
    public boolean isFlat() {
        return "flat".equals(funcName);
    }

    @Nullable
    @Override
    public String getFuncName() {
        return funcName;
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
        if (funcName != null) {
            builder.append(funcName).append('(').append(baseProp.getName()).append(')');
        } else {
            builder.append(baseProp.getName());
        }
        if (alias != null && !alias.equals(getKey())) {
            builder.append(" as ").append(alias);
        }
        if (targetType != null) {
            builder.append(": ");
            builder.append(targetType);
        }
        if (recursive) {
            builder.append('*');
        }
        return builder.toString();
    }

    static boolean canMerge(DtoProp<?, ?> p1, DtoProp<?, ?> p2) {

        if (p1.isIdOnly() != p2.isIdOnly()) {
            return false;
        }
        if (p1.isNullable() != p2.isNullable()) {
            return false;
        }

        if (p1.getTargetType() != null || p2.getTargetType() != null) {
            return false;
        }

        String alias1 = p1.getAlias();
        String alias2 = p2.getAlias();
        if (alias1 == null) {
            alias1 = p1.getName();
        }
        if (alias2 == null) {
            alias2 = p2.getName();
        }
        if (!alias1.equals(alias2)) {
            return false;
        }

        return true;
    }
}
