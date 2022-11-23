package org.babyfish.jimmer.meta.impl;

import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;
import kotlin.reflect.full.KClasses;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

class ImmutablePropImpl implements ImmutableProp {

    private final ImmutableTypeImpl declaringType;

    private final int id;

    private final String name;

    private final ImmutablePropCategory category;

    private final Class<?> elementClass;

    private final boolean nullable;

    private KProperty1<?, ?> kotlinProp;

    private Method javaGetter;

    private Annotation associationAnnotation;

    private final boolean isTransient;

    private final boolean hasTransientResolver;

    private final DissociateAction dissociateAction;

    private Storage storage;

    private boolean storageResolved;

    private ImmutableTypeImpl targetType;

    private boolean targetTypeResolved;

    private List<OrderedItem> orderedItems;

    private ImmutableProp mappedBy;

    private ImmutableProp acceptedMappedBy;

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

        KClass<?> kotlinClass = declaringType.getKotlinClass();
        if (kotlinClass != null) {
            kotlinProp = KClasses.getDeclaredMemberProperties(kotlinClass)
                    .stream()
                    .filter(it -> name.equals(it.getName()))
                    .findFirst()
                    .get();
        }
        try {
            javaGetter = declaringType.getJavaClass().getDeclaredMethod(name);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            javaGetter = declaringType.getJavaClass().getDeclaredMethod(
                    "get" + name.substring(0, 1).toUpperCase() + name.substring(1));
        } catch (NoSuchMethodException ignored) {
        }
        try {
            javaGetter = declaringType.getJavaClass().getDeclaredMethod(
                    "is" + name.substring(0, 1).toUpperCase() + name.substring(1));
            if (javaGetter.getReturnType() != boolean.class) {
                javaGetter = null;
            }
        } catch (NoSuchMethodException ignored) {
        }
        if (javaGetter == null) {
            throw new AssertionError(
                    "Internal bug: Cannot find the getter of prop \"" +
                            name +
                            "\" of the interface \"" +
                            declaringType.getJavaClass().getName() +
                            "\""
            );
        }

        Transient trans = getAnnotation(Transient.class);
        isTransient = trans != null;
        hasTransientResolver = trans != null && trans.value() != void.class;
        if (associationType != null) {

            associationAnnotation = getAnnotation(associationType);
        }

        OnDissociate onDissociate = getAnnotation(OnDissociate.class);
        if (onDissociate != null) {
            if (category != ImmutablePropCategory.REFERENCE) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", only reference property can be decorated by @OnDissociate"
                );
            }
            dissociateAction = onDissociate.value();
        } else {
            dissociateAction = DissociateAction.NONE;
        }
    }

    @NotNull
    @Override
    public ImmutableType getDeclaringType() {
        return declaringType;
    }

    @Override
    public int getId() {
        return id;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public ImmutablePropCategory getCategory() {
        return category;
    }

    @NotNull
    @Override
    public Class<?> getElementClass() {
        return elementClass;
    }

    @Override
    public boolean isScalar() {
        return this.category == ImmutablePropCategory.SCALAR;
    }

    @Override
    public boolean isScalarList() {
        return this.category == ImmutablePropCategory.SCALAR_LIST;
    }

    @Override
    public boolean isAssociation(TargetLevel level) {
        return this.category.isAssociation() &&
                (level == TargetLevel.OBJECT || !isTransient);
    }

    @Override
    public boolean isReference(TargetLevel level) {
        return this.category == ImmutablePropCategory.REFERENCE &&
                (level == TargetLevel.OBJECT || !isTransient);
    }

    @Override
    public boolean isReferenceList(TargetLevel level) {
        return this.category == ImmutablePropCategory.REFERENCE_LIST &&
                (level == TargetLevel.OBJECT || !isTransient);
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        if (kotlinProp != null) {
            Annotation annotation = kotlinProp
                    .getAnnotations()
                    .stream()
                    .filter(it -> it.annotationType() == annotationType)
                    .findFirst()
                    .orElse(null);
            if (annotation != null) {
                return (A) annotation;
            }
        }
        return javaGetter.getAnnotation(annotationType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
        A[] getterArr = javaGetter.getAnnotationsByType(annotationType);
        A[] propArr = null;
        if (kotlinProp != null) {
            propArr = (A[])kotlinProp
                    .getAnnotations()
                    .stream()
                    .filter(it -> it.annotationType() == annotationType)
                    .toArray();
        }
        if (propArr == null && propArr.length == 0) {
            return getterArr;
        }
        A[] mergedArr = (A[])new Object[propArr.length + getterArr.length];
        System.arraycopy(propArr, 0, mergedArr, 0, propArr.length);
        System.arraycopy(getterArr, 0, mergedArr, propArr.length, getterArr.length);
        return mergedArr;
    }

    @Override
    public Annotation getAssociationAnnotation() {
        return associationAnnotation;
    }

    @Override
    public boolean isTransient() {
        return isTransient;
    }

    @Override
    public boolean hasTransientResolver() {
        return hasTransientResolver;
    }

    @NotNull
    @Override
    public DissociateAction getDissociateAction() {
        return dissociateAction;
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

    @Override
    public boolean isId() {
        return this == declaringType.getIdProp();
    }

    @Override
    public boolean isVersion() {
        return this == declaringType.getVersionProp();
    }

    @Override
    public ImmutableType getTargetType() {
        if (targetTypeResolved) {
            return targetType;
        }
        if (isAssociation(TargetLevel.OBJECT)) {
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

    @Override
    public List<OrderedItem> getOrderedItems() {
        List<OrderedItem> orderedItems = this.orderedItems;
        if (orderedItems == null) {
            OrderedProp[] orderedProps = null;
            if (isReferenceList(TargetLevel.ENTITY)) {
                OneToMany oneToMany = getAnnotation(OneToMany.class);
                if (oneToMany != null) {
                    orderedProps = oneToMany.orderedProps();
                } else {
                    ManyToMany manyToMany = getAnnotation(ManyToMany.class);
                    if (manyToMany != null) {
                        orderedProps = manyToMany.orderedProps();
                    }
                }
            }
            if (orderedProps == null || orderedProps.length == 0) {
                orderedItems = Collections.emptyList();
            } else {
                ImmutableType targetType = getTargetType();
                Map<String, OrderedItem> map = new LinkedHashMap<>((orderedProps.length * 4 + 2) / 3);
                for (OrderedProp orderedProp : orderedProps) {
                    if (map.containsKey(orderedProp.value())) {
                        throw new ModelException(
                                "Illegal property \"" +
                                        this +
                                        "\", duplicated ordered property \"" +
                                        orderedProp.value() +
                                        "\""
                        );
                    }
                    ImmutableProp prop = targetType.getProp(orderedProp.value());
                    if (prop == null) {
                        throw new ModelException(
                                "Illegal property \"" +
                                        this +
                                        "\", the ordered property \"" +
                                        orderedProp.value() +
                                        "\" is not declared in target type \"" +
                                        targetType +
                                        "\""
                        );
                    }
                    if (!prop.isScalar()) {
                        throw new ModelException(
                                "Illegal property \"" +
                                        this +
                                        "\", the ordered property \"" +
                                        prop +
                                        "\" is not scalar field"
                        );
                    }
                    map.put(orderedProp.value(), new OrderedItem(prop, orderedProp.desc()));
                }
                orderedItems = Collections.unmodifiableList(new ArrayList<>(map.values()));
            }
            this.orderedItems = orderedItems;
        }
        return orderedItems;
    }

    @Override
    public ImmutableProp getMappedBy() {
        if (mappedByResolved) {
            return mappedBy;
        }
        if (isAssociation(TargetLevel.ENTITY)) {
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
                if (resolved.getStorage() == null) {
                    throw new ModelException(
                            "The property \"" +
                                    resolved +
                                    "\" is illegal, it's not persistence property so that " +
                                    "\"" +
                                    "this" +
                                    "\" cannot reference it by \"mappedBy\""
                    );
                }
                if (resolved.getAssociationAnnotation().annotationType() == OneToOne.class &&
                        associationAnnotation.annotationType() != OneToOne.class
                ) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it must be one-to-one property " +
                                    "because its \"mappedBy\" property \"" +
                                    resolved +
                                    "\" is one-to-one property"
                    );
                }
                if (resolved.getAssociationAnnotation().annotationType() == ManyToOne.class &&
                        associationAnnotation.annotationType() != OneToMany.class
                ) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it must be one-to-one property " +
                                    "because its \"mappedBy\" property \"" +
                                    resolved +
                                    "\" is one-to-one property"
                    );
                }
                if (resolved.isReferenceList(TargetLevel.ENTITY) &&
                        associationAnnotation.annotationType() != ManyToMany.class
                ) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it must be many-to-many property " +
                                    "because its \"mappedBy\" property \"" +
                                    resolved +
                                    "\" is list"
                    );
                }
                ((ImmutablePropImpl)resolved).acceptMappedBy(this);
                this.mappedBy = resolved;
            }
        }
        mappedByResolved = true;
        return mappedBy;
    }

    private void acceptMappedBy(ImmutableProp prop) {
        if (acceptedMappedBy != null) {
            throw new ModelException(
                    "Both `" +
                            acceptedMappedBy +
                            "` and `" +
                            prop +
                            "` use `mappedBy` to reference `" +
                            this +
                            "`"
            );
        }
        acceptedMappedBy = prop;
    }

    @Override
    public ImmutableProp getOpposite() {
        if (oppositeResolved) {
            return opposite;
        }
        if (isAssociation(TargetLevel.ENTITY)) {
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
    public boolean equals(Object o) {
        return o instanceof ImmutableProp && this == RedirectedProp.unwrap((ImmutableProp) o);
    }

    @Override
    public String toString() {
        return declaringType.toString() + '.' + name;
    }
}
