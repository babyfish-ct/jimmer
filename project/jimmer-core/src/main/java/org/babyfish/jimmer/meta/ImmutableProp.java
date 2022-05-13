package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.meta.sql.Storage;

import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class ImmutableProp {

    private ImmutableType declaringType;

    private String name;

    private ImmutablePropCategory category;

    private Class<?> elementClass;

    private boolean nullable;

    private Method getter;

    private Annotation associationAnnotation;

    private boolean isTransient;

    private Storage storage;

    private boolean storageResolved;

    private boolean isId;

    private boolean isIdResolved;

    private ImmutableType targetType;

    private boolean targetTypeResolved;

    private ImmutableProp mappedBy;

    private boolean mappedByResolved;

    private ImmutableProp opposite;

    private boolean oppositeResolved;

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

        try {
            getter = declaringType.getJavaClass().getDeclaredMethod(name);
        } catch (NoSuchMethodException ex) {
        }
        try {
            getter = declaringType.getJavaClass().getDeclaredMethod(
                    "get" + name.substring(0).toUpperCase() + name.substring(1));
        } catch (NoSuchMethodException ex) {
        }
        try {
            getter = declaringType.getJavaClass().getDeclaredMethod(
                    "is" + name.substring(0).toUpperCase() + name.substring(1));
        } catch (NoSuchMethodException ex) {
        }
        if (getter == null) {
            throw new AssertionError(
                    "Internal bug: Cannot find the getter of prop \"" +
                            name +
                            "\" of the interface \"" +
                            declaringType.getJavaClass().getName() +
                            "\""
            );
        }

        isTransient = getAnnotation(Transient.class) != null;
        if (associationType != null) {
            associationAnnotation = getAnnotation(associationType);
        }
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

    public Method getGetter() {
        return getter;
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return getter.getAnnotation(annotationType);
    }

    public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
        return getter.getAnnotationsByType(annotationType);
    }

    public Annotation getAssociationAnnotation() {
        return associationAnnotation;
    }

    public boolean isTransient() {
        return isTransient;
    }

    @SuppressWarnings("unchecked")
    public <S extends Storage> S getStorage() {
        if (storageResolved) {
            return (S)storage;
        }
        storage = Storages.of(this);
        storageResolved = true;
        return (S)storage;
    }

    public boolean isId() {
        if (isIdResolved) {
            return isId;
        }
        isIdResolved = true;
        ImmutableProp idProp = declaringType.getIdProp();
        isId = idProp == this || (idProp != null && idProp.getName().equals(name));
        return isId;
    }

    public ImmutableType getTargetType() {
        if (targetTypeResolved) {
            return targetType;
        }
        if (isAssociation()) {
            targetType = ImmutableType.tryGet(elementClass);
            if (targetType == null) {
                throw new ModelException(
                        "Cannot resolve target type of \"" +
                                this +
                                "\""
                );
            }
        }
        targetTypeResolved = true;
        return targetType;
    }

    public ImmutableProp getMappedBy() {
        if (mappedByResolved) {
            return mappedBy;
        }
        if (isAssociation()) {
            String mappedBy = "";
            OneToOne oneToOne = getAnnotation(OneToOne.class);
            if (oneToOne != null) {
                mappedBy = oneToOne.mappedBy();
            }
            if (mappedBy.isEmpty()) {
                OneToMany oneToMany = getAnnotation(OneToMany.class);
                if (oneToMany != null) {
                    mappedBy = oneToMany.mappedBy();
                }
                if (mappedBy.isEmpty()) {
                    ManyToMany manyToMany = getAnnotation(ManyToMany.class);
                    if (manyToMany != null) {
                        mappedBy = manyToMany.mappedBy();
                    }
                }
            }
            if (!mappedBy.isEmpty()) {
                ImmutableProp resolved = getTargetType().getProps().get(mappedBy);
                if (resolved == null) {
                    throw new ModelException(
                            "Cannot resolve the mappedBy property name \"" +
                                    mappedBy +
                                    "\" for property \"" +
                                    this +
                                    "\""
                    );
                }
                this.mappedBy = resolved;
            }
        }
        mappedByResolved = true;
        return mappedBy;
    }

    public ImmutableProp getOpposite() {
        if (oppositeResolved) {
            return opposite;
        }
        if (isAssociation()) {
            opposite = getMappedBy();
            if (opposite == null) {
                for (ImmutableProp backProp : getTargetType().getProps().values()) {
                    if (backProp.getMappedBy() == this) {
                        opposite = backProp;
                        break;
                    }
                }
            }
        }
        oppositeResolved = true;
        return opposite;
    }

    @Override
    public String toString() {
        return declaringType.toString() + '.' + name;
    }
}
