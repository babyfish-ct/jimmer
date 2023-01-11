package org.babyfish.jimmer.apt.meta;

public class StaticDeclaration {

    private final ImmutableType immutableType;

    private final String alias;

    private final String topLevelName;

    private final boolean allScalars;

    private final boolean allOptional;

    public StaticDeclaration(
            ImmutableType immutableType,
            String alias,
            String topLevelName,
            boolean allScalars,
            boolean allOptional) {
        this.immutableType = immutableType;
        this.alias = alias;
        this.topLevelName = topLevelName;
        this.allScalars = allScalars;
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

    public boolean isAllScalars() {
        return allScalars;
    }

    public boolean isAllOptional() {
        return allOptional;
    }

    public StaticDeclaration rename(String name) {
        if (this.topLevelName.equals(name)) {
            return this;
        }
        return new StaticDeclaration(immutableType, alias, name, allScalars, allOptional);
    }

    @Override
    public String toString() {
        return "StaticDeclaration{" +
                "immutableType=" + immutableType +
                ", alias='" + alias + '\'' +
                ", topLevelName='" + topLevelName + '\'' +
                ", allScalars=" + allScalars +
                ", allOptional=" + allOptional +
                '}';
    }
}
