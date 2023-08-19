package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class DtoPropImpl<T extends BaseType, P extends BaseProp> implements DtoProp<T, P> {

    private final P baseProp;

    @Nullable
    private final DtoProp<T, P> nextProp;

    private final int baseLine;

    @Nullable
    private final String alias;

    private final int aliasLine;

    private final DtoType<T, P> targetType;

    private final boolean optional;

    private final String funcName;

    private final boolean recursive;

    private final String basePath;

    private final DtoProp<T, P> tail;

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
        this.nextProp = null;
        this.baseLine = baseLine;
        this.alias = alias;
        this.aliasLine = aliasLine;
        this.targetType = targetType;
        this.optional = optional;
        this.funcName = funcName;
        this.recursive = recursive;
        this.basePath = baseProp.getName();
        this.tail = this;
    }

    DtoPropImpl(DtoProp<T, P> head, DtoProp<T, P> next) {
        this.baseProp = head.getBaseProp();
        this.nextProp = next;
        this.baseLine = next.getBaseLine();
        this.alias = next.getAlias();
        this.aliasLine = next.getAliasLine();
        this.targetType = next.getTargetType();
        this.optional = head.isNullable() || next.isNullable();
        this.funcName = next.getFuncName();
        this.recursive = false;
        StringBuilder builder = new StringBuilder(baseProp.getName());
        DtoProp<T, P> tail = this;
        for (DtoProp<T, P> n = next; n != null; n = n.getNextProp()) {
            builder.append('.').append(n.getBaseProp().getName());
            tail = n;
        }
        this.basePath = builder.toString();
        this.tail = tail;
    }

    DtoPropImpl(DtoProp<T, P> original, DtoType<T, P> targetType) {
        this.baseProp = original.getBaseProp();
        this.nextProp = null;
        this.baseLine = original.getBaseLine();
        this.alias = baseProp.getName();
        this.aliasLine = original.getAliasLine();
        this.targetType = targetType;
        this.optional = false;
        this.funcName = "flat";
        this.recursive = false;
        this.basePath = baseProp.getName();
        this.tail = this;
    }

    @Override
    public P getBaseProp() {
        return baseProp;
    }

    @Override
    public String getBasePath() {
        return basePath;
    }

    @Nullable
    @Override
    public DtoProp<T, P> getNextProp() {
        return nextProp;
    }

    @Override
    public DtoProp<T, P> toTailProp() {
        return tail;
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
            builder.append(funcName).append('(').append(basePath).append(')');
        } else {
            builder.append(basePath);
        }
        if (alias != null && !alias.equals(tail.getBaseProp().getName())) {
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
        if (p1.isNullable() != p2.isNullable()) {
            return false;
        }
        if (!p1.getBasePath().equals(p2.getBaseProp())) {
            return false;
        }
        if (!Objects.equals(p1.getFuncName(), p2.getFuncName())) {
            return false;
        }
        if (p1.getTargetType() != null) {
            return false;
        }
        return true;
    }
}
