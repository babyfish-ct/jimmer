package org.babyfish.jimmer.apt.meta;

public class StaticDeclaration {

    private final ImmutableType dynamicType;

    private final String alias;

    private final String topLevelName;

    private final boolean allScalars;

    private final boolean isMutable;

    public StaticDeclaration(
            ImmutableType dynamicType,
            String alias,
            String topLevelName,
            boolean allScalars,
            boolean isMutable
    ) {
        this.dynamicType = dynamicType;
        this.alias = alias;
        this.topLevelName = topLevelName;
        this.allScalars = allScalars;
        this.isMutable = isMutable;
    }

    public ImmutableType getDynamicType() {
        return dynamicType;
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

    public boolean isMutable() {
        return isMutable;
    }

    public StaticDeclaration rename(String name) {
        if (this.topLevelName.equals(name)) {
            return this;
        }
        return new StaticDeclaration(dynamicType, alias, name, allScalars, isMutable);
    }

    @Override
    public String toString() {
        return "StaticDeclaration{" +
                "alias='" + alias + '\'' +
                ", name='" + topLevelName + '\'' +
                ", allScalars=" + allScalars +
                ", isMutable=" + isMutable +
                '}';
    }
}
