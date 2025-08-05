package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.impl.org.objectweb.asm.tree.InsnList;

public class WeakJoinLambda {

    private final InsnList instructions;

    private final Class<?> sourceType;

    private final Class<?> targetType;

    private int hashCache;

    public WeakJoinLambda(
            InsnList instructions,
            Class<?> sourceType,
            Class<?> targetType
    ) {
        this.instructions = instructions;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public Class<?> getSourceType() {
        return sourceType;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    @Override
    public int hashCode() {
        int h = hashCache;
        if (h == 0) {
            h = hashCode0();
            if (h == 0) {
                h = -1;
            }
            hashCache = h;
        }
        return h;
    }

    private int hashCode0() {
        int result = sourceType.hashCode();
        result = 31 * result + targetType.hashCode();
        result = 31 * result + InsnListUtils.hashCode(instructions);
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof WeakJoinLambda)) {
            return false;
        }
        WeakJoinLambda that = (WeakJoinLambda) o;
        return sourceType.equals(that.sourceType) &&
                targetType.equals(that.targetType) &&
                InsnListUtils.equals(instructions, that.instructions);
    }

    @Override
    public String toString() {
        return "WeakJoinMetadata{" +
                "instructions=" + instructions +
                ", sourceType=" + sourceType +
                ", targetType=" + targetType +
                '}';
    }
}
