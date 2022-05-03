package org.babyfish.jimmer.meata;

public class ImmutableProp {

    private ImmutableType declaringType;

    private String name;

    private ImmutablePropCategory category;

    private Class<?> elementClass;

    private boolean nullable;

    ImmutableProp(
            ImmutableType declaringType,
            String name,
            ImmutablePropCategory category,
            Class<?> elementClass,
            boolean nullable
    ) {
        this.declaringType = declaringType;
        this.name = name;
        this.category = category;
        this.elementClass = elementClass;
    }

    public ImmutableType getDeclaringType() {
        return declaringType;
    }

    public String getName() {
        return name;
    }

    public ImmutablePropCategory getCategory() {
        return category;
    }

    public Class<?> getElementClass() {
        return elementClass;
    }

    public boolean isScalar() {
        return this.category == ImmutablePropCategory.SCALAR;
    }

    public boolean isScalarList() {
        return this.category == ImmutablePropCategory.SCALAR_LIST;
    }

    public boolean isAssociation() {
        return this.category.isAssociation();
    }

    public boolean isReference() {
        return this.category == ImmutablePropCategory.REFERENCE;
    }

    public boolean isEntityList() {
        return this.category == ImmutablePropCategory.ENTITY_LIST;
    }

    @Override
    public String toString() {
        return declaringType.toString() + '.' + name;
    }
}
