package org.babyfish.jimmer.apt.meta;

public class StaticDeclaration {

    private final ImmutableType immutableType;

    private final String alias;

    private final String topLevelName;

    private final boolean allOptional;

    public StaticDeclaration(
            ImmutableType immutableType,
            String alias,
            String topLevelName,
            boolean allOptional
    ) {
        this.immutableType = immutableType;
        this.alias = alias;
        this.topLevelName = topLevelName;
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

    public boolean isAllOptional() {
        return allOptional;
    }

    @Override
    public String toString() {
        return "StaticDeclaration{" +
                "immutableType=" + immutableType +
                ", alias='" + alias + '\'' +
                ", topLevelName='" + topLevelName + '\'' +
                ", allOptional=" + allOptional +
                '}';
    }
}
