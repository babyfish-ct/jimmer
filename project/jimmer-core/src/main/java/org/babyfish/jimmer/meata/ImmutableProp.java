package org.babyfish.jimmer.meata;

import java.lang.annotation.Annotation;

public class ImmutableProp {

    private ImmutableType declaringType;

    private String name;

    private ImmutablePropCategory category;

    private Class<?> elementClass;

    private boolean nullable;

    private Class<? extends Annotation> associationType;

    ImmutableProp(
            ImmutableType declaringType,
            String name,
            ImmutablePropCategory category,
            Class<?> elementClass,
            boolean nullable,
            Class<? extends Annotation> associationType
    ) {
        this.declaringType = declaringType;
        this.name = name;
        this.category = category;
        this.elementClass = elementClass;
        this.nullable = nullable;
        this.associationType = associationType;
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

    public boolean isNullable() {
        return nullable;
    }

    public Class<? extends Annotation> getAssociationType() {
        return associationType;
    }

    @Override
    public String toString() {
        return declaringType.toString() + '.' + name;
    }
}
