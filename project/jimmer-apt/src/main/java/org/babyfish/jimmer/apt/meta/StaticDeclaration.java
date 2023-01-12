package org.babyfish.jimmer.apt.meta;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;

public class StaticDeclaration {

    private final ImmutableType immutableType;

    private final String alias;

    private final String topLevelName;

    private final AutoScalarStrategy autoScalarStrategy;

    private final boolean allOptional;

    public StaticDeclaration(
            ImmutableType immutableType,
            String alias,
            String topLevelName,
            AutoScalarStrategy autoScalarStrategy,
            boolean allOptional) {
        this.immutableType = immutableType;
        this.alias = alias;
        this.topLevelName = topLevelName;
        this.autoScalarStrategy = autoScalarStrategy;
        this.allOptional = allOptional;
    }

    public ImmutableType getImmutableType() {
        return immutableType;
    }

    public String getAlias() {
        return alias;
    }

    public String getTopLevelName() {
        return topLevelName;
    }

    public AutoScalarStrategy getAutoScalarStrategy() {
        return autoScalarStrategy;
    }

    public boolean isAllOptional() {
        return allOptional;
    }

    @Override
    public String toString() {
        return "StaticDeclaration{" +
                "immutableType=" + immutableType +
                ", alias='" + alias + '\'' +
                ", topLevelName='" + topLevelName + '\'' +
                ", autoScalarStrategy=" + autoScalarStrategy +
                ", allOptional=" + allOptional +
                '}';
    }
}
