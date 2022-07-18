package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutablePropCategory;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.DeleteAction;
import org.babyfish.jimmer.sql.OnDelete;
import org.babyfish.jimmer.sql.meta.Storage;

import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

class ImmutablePropImpl implements ImmutableProp {

    private final ImmutableTypeImpl declaringType;

    private final int id;

    private final String name;

    private final ImmutablePropCategory category;

    private final Class<?> elementClass;

    private final boolean nullable;

    private Method getter;

    private Annotation associationAnnotation;

    private final boolean isTransient;

    private final DeleteAction deleteAction;

    private Storage storage;

    private boolean storageResolved;

    private ImmutableTypeImpl targetType;

    private boolean targetTypeResolved;

    private ImmutableProp mappedBy;

    private boolean mappedByResolved;

    private ImmutableProp opposite;

    private boolean oppositeResolved;

    ImmutablePropImpl(
            ImmutableTypeImpl declaringType,
            int id,
            String name,
            ImmutablePropCategory category,
            Class<?> elementClass,
            boolean nullable,
            Class<? extends Annotation> associationType
    ) {
        this.declaringType = declaringType;
        this.id = id;
        this.name = name;
        this.category = category;
        this.elementClass = elementClass;
        this.nullable = nullable;

        try {
            getter = declaringType.getJavaClass().getDeclaredMethod(name);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            getter = declaringType.getJavaClass().getDeclaredMethod(
                    "get" + name.substring(0, 1).toUpperCase() + name.substring(1));
        } catch (NoSuchMethodException ignored) {
        }
        try {
            getter = declaringType.getJavaClass().getDeclaredMethod(
                    "is" + name.substring(0, 1).toUpperCase() + name.substring(1));
        } catch (NoSuchMethodException ignored) {
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

        OnDelete onDelete = getAnnotation(OnDelete.class);
        deleteAction = onDelete != null ? onDelete.value() : DeleteAction.NONE;
    }

    public ImmutableType getDeclaringType() {
        return declaringType;
    }

    public int getId() {
        return id;
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

    public DeleteAction getDeleteAction() {
        return deleteAction;
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
        return this == declaringType.getIdProp();
    }

    public boolean isVersion() {
        return this == declaringType.getVersionProp();
    }

    public ImmutableType getTargetType() {
        if (targetTypeResolved) {
            return targetType;
        }
        if (isAssociation()) {
            targetType = Metadata.tryGet(elementClass);
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
                if (resolved.isReference() &&
                        associationAnnotation.annotationType() != OneToOne.class &&
                        associationAnnotation.annotationType() != OneToMany.class
                ) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it must be one-to-one of one-to-many property " +
                                    "because its mappedBy property \"" +
                                    resolved +
                                    "\" is reference"
                    );
                }
                if (resolved.isEntityList() &&
                        associationAnnotation.annotationType() != ManyToMany.class
                ) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it must be many-to-many property " +
                                    "because its mappedBy property \"" +
                                    resolved +
                                    "\" is list"
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
