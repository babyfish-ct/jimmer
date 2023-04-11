package org.babyfish.jimmer.meta.impl;

import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;
import kotlin.reflect.full.KClasses;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.meta.spi.EntityPropImplementor;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

class ImmutablePropImpl implements ImmutableProp, EntityPropImplementor {

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    private final ImmutableTypeImpl declaringType;

    private final int id;

    private final String name;

    private final ImmutablePropCategory category;

    private final Class<?> elementClass;

    private final boolean nullable;

    private final boolean inputNotNull;

    private final KProperty1<?, ?> kotlinProp;

    private final Method javaGetter;

    private final Annotation associationAnnotation;

    private final boolean isTransient;

    private final boolean hasTransientResolver;

    private final boolean isFormula;

    private final FormulaTemplate formulaTemplate;

    private final DissociateAction dissociateAction;

    private final ImmutableProp base;

    private Converter<?> converter;

    private boolean converterResolved;

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

    private List<Dependency> dependencies;

    private ImmutableProp idViewBaseProp;

    private boolean idViewBasePropResolved;

    private ImmutableProp manyToManyViewBaseProp;

    private ImmutableProp manyToManyViewBaseDeeperProp;

    private boolean manyToManyViewBasePropResolved;

    private Boolean isRemote;

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
        } else {
            kotlinProp = null;
        }
        Method javaGetter = null;
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
        this.javaGetter = javaGetter;

        Transient trans = getAnnotation(Transient.class);
        isTransient = trans != null;
        hasTransientResolver = trans != null && (
                trans.value() != void.class || !trans.ref().isEmpty()
        );
        if (associationType != null) {
            associationAnnotation = getAnnotation(associationType);
        } else {
            associationAnnotation = null;
        }

        Formula formula = getAnnotation(Formula.class);
        isFormula = formula != null;
        if (formula != null && isAssociation(TargetLevel.ENTITY)) {
            throw new ModelException(
                    "Illegal property \"" +
                            this +
                            "\", it is decorated by \"" +
                            Formula.class.getName() +
                            "\" so that it cannot be association"
            );
        }
        if (formula != null && !formula.sql().isEmpty()) {
            try {
                formulaTemplate = FormulaTemplate.of(formula.sql());
            } catch (IllegalArgumentException ex) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", the formula sql template: " +
                                ex.getMessage()
                );
            }
        } else {
            formulaTemplate = null;
        }

        ManyToOne manyToOne = getAnnotation(ManyToOne.class);
        OneToOne oneToOne = getAnnotation(OneToOne.class);
        inputNotNull = manyToOne != null ?
                manyToOne.inputNotNull() :
                oneToOne != null && oneToOne.inputNotNull();
        if (isInputNotNull() && !nullable) {
            throw new ModelException(
                    "Illegal property \"" +
                            this +
                            "\", it `inputNotNull` can only be specified for nullable property"
            );
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
            if (oneToOne != null && !oneToOne.mappedBy().isEmpty()) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", the one-to-one property with `mappedBy` cannot be decorated by @OnDissociate"
                );
            }
            dissociateAction = onDissociate.value();
        } else {
            dissociateAction = DissociateAction.NONE;
        }

        this.base = null;
    }

    ImmutablePropImpl(
            ImmutableTypeImpl declaringType,
            ImmutablePropImpl base
    ) {
        if (!base.getDeclaringType().isAssignableFrom(declaringType)) {
            throw new IllegalArgumentException(
                    "The new declaring type \"" +
                            declaringType +
                            "\" is illegal, it is not derived type of original declaring type \"" +
                            base.getDeclaringType() +
                            "\""
            );
        }
        this.declaringType = declaringType;
        this.id = base.id;
        this.name = base.name;
        this.category = base.category;
        this.elementClass = base.elementClass;
        this.nullable = base.nullable;
        this.inputNotNull = base.inputNotNull;
        this.kotlinProp = base.kotlinProp;
        this.javaGetter = base.javaGetter;
        this.associationAnnotation = base.associationAnnotation;
        this.isTransient = base.isTransient;
        this.isFormula = base.isFormula;
        this.formulaTemplate = base.formulaTemplate;
        this.hasTransientResolver = base.hasTransientResolver;
        this.dissociateAction = base.dissociateAction;
        this.base = base.base != null ? base.base : base;
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
    public boolean isEmbedded(EmbeddedLevel level) {
        ImmutableType targetType = getTargetType();
        if (level.hasReference() &&
                isReference(TargetLevel.PERSISTENT) &&
                targetType.getIdProp().isEmbedded(EmbeddedLevel.SCALAR)
        ) {
            return true;
        }
        return level.hasScalar() && targetType != null && targetType.isEmbeddable();
    }

    @Override
    public boolean isScalar(TargetLevel level) {
        if (level == TargetLevel.OBJECT) {
            return category == ImmutablePropCategory.SCALAR;
        }
        ImmutableType targetType = getTargetType();
        return targetType == null || !targetType.isEntity();
    }

    @Override
    public boolean isScalarList() {
        return this.category == ImmutablePropCategory.SCALAR_LIST;
    }

    @Override
    public boolean isAssociation(TargetLevel level) {
        if (!this.category.isAssociation()) {
            return false;
        }
        int ordinal = level.ordinal();
        if (ordinal >= TargetLevel.ENTITY.ordinal() && !getTargetType().isEntity()) {
            return false;
        }
        if (ordinal >= TargetLevel.PERSISTENT.ordinal() && (isTransient || isRemote())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isReference(TargetLevel level) {
        return this.category == ImmutablePropCategory.REFERENCE && isAssociation(level);
    }

    @Override
    public boolean isReferenceList(TargetLevel level) {
        return this.category == ImmutablePropCategory.REFERENCE_LIST && isAssociation(level);
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    public boolean isInputNotNull() {
        return inputNotNull;
    }

    @Override
    public boolean isMutable() {
        return !isFormula || formulaTemplate != null;
    }

    @Override
    public Method getJavaGetter() {
        return javaGetter;
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
    public Annotation[] getAnnotations() {
        Annotation[] getterArr = javaGetter.getAnnotations();
        Annotation[] propArr = null;
        if (kotlinProp != null) {
            propArr = kotlinProp.getAnnotations().toArray(EMPTY_ANNOTATIONS);
        }
        if (propArr == null || propArr.length == 0) {
            return getterArr;
        }
        Annotation[] mergedArr = new Annotation[propArr.length + getterArr.length];
        System.arraycopy(propArr, 0, mergedArr, 0, propArr.length);
        System.arraycopy(getterArr, 0, mergedArr, propArr.length, getterArr.length);
        return mergedArr;
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
        if (propArr == null || propArr.length == 0) {
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

    @Override
    public boolean isFormula() {
        return isFormula;
    }

    @Nullable
    @Override
    public FormulaTemplate getFormulaTemplate() {
        return formulaTemplate;
    }

    @Override
    public boolean isView() {
        return idViewBaseProp != null || idViewBaseProp != null;
    }

    @Override
    public ImmutableProp getIdViewBaseProp() {
        if (idViewBasePropResolved) {
            return idViewBaseProp;
        }
        ImmutableProp baseProp;
        IdView idView = getAnnotation(IdView.class);
        if (idView == null) {
            baseProp = null;
        } else {
            if (isAssociation(TargetLevel.ENTITY)) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", it is decorated by \"" +
                                IdView.class.getName() +
                                "\" so that it cannot be association"
                );
            }
            String basePropName;
            if (idView.value().isEmpty()) {
                if (name.length() < 3 || !name.endsWith("Id") || Character.isUpperCase(name.charAt(name.length() - 3))) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it is decorated by \"" +
                                    IdView.class.getName() +
                                    "\" but the base property name cannot be determined automatically"
                    );
                }
                basePropName = name.substring(0, name.length() - 2);
            } else {
                basePropName = idView.value();
            }
            baseProp = declaringType.getProps().get(basePropName);
            if (baseProp == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", it is decorated by \"" +
                                IdView.class.getName() +
                                "\" but there is not base property named \"" +
                                basePropName +
                                "\" in the type \"" +
                                declaringType +
                                "\""
                );
            }
            if (baseProp.isTransient()) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\" is a scalar list property, it is decorated by \"" +
                                IdView.class.getName() +
                                "\" whose argument of that annotation is \"" +
                                basePropName +
                                "\" but the base property \"" +
                                baseProp +
                                "\" is not a reference list property"
                );
            } else if (isScalarList() && !baseProp.isReferenceList(TargetLevel.ENTITY)) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\" is a scalar list property, it is decorated by \"" +
                                IdView.class.getName() +
                                "\" whose argument of that annotation is \"" +
                                basePropName +
                                "\" but the base property \"" +
                                baseProp +
                                "\" is not a reference list property"
                );
            } else if (!isScalarList() && !baseProp.isReference(TargetLevel.ENTITY)) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\" is a scalar property, it is decorated by \"" +
                                IdView.class.getName() +
                                "\" whose argument of that annotation is \"" +
                                basePropName +
                                "\" but the base property \"" +
                                baseProp +
                                "\" is not a reference property"
                );
            }
            if (!Classes.matches(baseProp.getTargetType().getIdProp().getElementClass(), getElementClass())) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\" is a scalar property, it is decorated by \"" +
                                IdView.class.getName() +
                                "\" whose argument of that annotation is \"" +
                                basePropName +
                                "\", the base property \"" +
                                baseProp +
                                "\" return the entity type whose id is \"" +
                                baseProp.getTargetType().getIdProp().getElementClass() +
                                "\" but the element type of the current property is \"" +
                                getElementClass() +
                                "\""
                );
            }
            if (isNullable() != baseProp.isNullable()) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\" is a scalar property, it is decorated by \"" +
                                IdView.class.getName() +
                                "\" whose argument of that annotation is \"" +
                                basePropName +
                                "\", but the nullity of current property does not equal to the nullity of the base property \"" +
                                baseProp +
                                "\""
                );
            }
        }
        idViewBaseProp = baseProp;
        idViewBasePropResolved = true;
        return baseProp;
    }

    @Override
    public ImmutableProp getManyToManyViewBaseProp() {
        resolveManyToManyViewBaseProp();
        return manyToManyViewBaseProp;
    }

    @Override
    public ImmutableProp getManyToManyViewBaseDeeperProp() {
        resolveManyToManyViewBaseProp();
        return manyToManyViewBaseDeeperProp;
    }

    private void resolveManyToManyViewBaseProp() {
        if (manyToManyViewBasePropResolved) {
            return;
        }
        ManyToManyView manyToManyView = getAnnotation(ManyToManyView.class);
        if (manyToManyView == null) {
            return;
        }
        String propName = manyToManyView.prop();
        ImmutableProp prop = declaringType.getProps().get(propName);
        if (prop == null) {
            throw new ModelException(
                    "Illegal property \"" +
                            this +
                            "\", it is decorated by \"" +
                            ManyToManyView.class.getName() +
                            "\" but there is not base property named \"" +
                            propName +
                            "\" in the type \"" +
                            declaringType +
                            "\""
            );
        }
        if (prop.getAnnotation(OneToMany.class) == null) {
            throw new ModelException(
                    "Illegal property \"" +
                            this +
                            "\", it is decorated by \"" +
                            ManyToManyView.class.getName() +
                            "\" with `prop` \"" +
                            propName +
                            "\", but prop \"" +
                            prop +
                            "\" in the type is not an one-to-many association"
            );
        }
        ImmutableType middleType = prop.getTargetType();
        String deeperPropName = manyToManyView.deeperProp();
        ImmutableProp deeperProp = null;
        if (deeperPropName.isEmpty()) {
            for (ImmutableProp middleProp : middleType.getProps().values()) {
                if (middleProp.getTargetType() == getTargetType() &&
                middleProp.getAnnotation(ManyToOne.class) != null) {
                    if (deeperProp != null) {
                        throw new ModelException(
                                "Illegal property \"" +
                                        this +
                                        "\", it is decorated by \"" +
                                        ManyToManyView.class.getName() +
                                        "\" but the middle entity has two many-to-one " +
                                        "association properties pointing to target type: \"" +
                                        deeperProp +
                                        "\" and \"" +
                                        middleProp +
                                        "\""
                        );
                    }
                    deeperProp = middleProp;
                }
            }
            if (deeperProp == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", it is decorated by \"" +
                                ManyToManyView.class.getName() +
                                "\" but the middle entity \"" +
                                middleType +
                                "\" has no many-to-one " +
                                "association properties pointing to target type: "
                );
            }
        } else {
            deeperProp = middleType.getProps().get(deeperPropName);
            if (deeperProp == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", it is decorated by \"" +
                                ManyToManyView.class.getName() +
                                "\" but there is not property \"" +
                                deeperPropName +
                                "\" in the middle entity type: " +
                                middleType
                );
            }
        }
        if (deeperProp.getAnnotation(ManyToOne.class) == null ||
                deeperProp.getTargetType() != getTargetType()) {
            throw new ModelException(
                    "Illegal property \"" +
                            this +
                            "\", it is decorated by \"" +
                            ManyToManyView.class.getName() +
                            "\" but the deeper property \"" +
                            deeperProp +
                            "\" is not a many-to-one proerty pointing to the target type \"" +
                            getTargetType() +
                            "\""
            );
        }
        manyToManyViewBaseProp = prop;
        manyToManyViewBaseDeeperProp = deeperProp;
        manyToManyViewBasePropResolved = true;
    }

    @Override
    public Converter<?> getConverter() {
        if (converterResolved) {
            return converter;
        }
        Class<? extends Annotation> annotationType = null;
        JsonConverter jsonConverter = getAnnotation(JsonConverter.class);
        if (jsonConverter != null) {
            annotationType = JsonConverter.class;
        }
        for (Annotation anno : getAnnotations()) {
            if (anno.annotationType() != JsonConverter.class) {
                JsonConverter deepAnno = anno.annotationType().getAnnotation(JsonConverter.class);
                if (deepAnno != null) {
                    if (annotationType != null) {
                        throw new ModelException(
                                "Illegal property \"" +
                                        this +
                                        "\", duplicate converter annotation @" +
                                        annotationType.getName() +
                                        " and @" +
                                        anno.annotationType().getName()
                        );
                    }
                    jsonConverter = deepAnno;
                    annotationType = anno.annotationType();
                }
            }
        }
        if (jsonConverter != null) {
            Class<? extends Converter<?>> converterType = jsonConverter.value();
            Collection<Type> genericArguments = TypeUtils.getTypeArguments(converterType, Converter.class).values();
            if (genericArguments.isEmpty() || !(genericArguments.iterator().next() instanceof Class<?>)) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", it cannot be decorated by @" +
                                annotationType.getName() +
                                ", the converter type \"" +
                                converterType.getName() +
                                "\" does not specify the generic parameter of \"" +
                                Converter.class.getName() +
                                "\" as class"
                );
            }
            Class<?> convertedType = (Class<?>)genericArguments.iterator().next();
            if (convertedType != javaGetter.getReturnType()) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", it cannot be decorated by @" +
                                annotationType.getName() +
                                ", the property type \"" +
                                javaGetter.getReturnType() +
                                "\" does not match the generic type \"" +
                                convertedType.getName() +
                                "\" of converter type \"" +
                                converterType.getName() +
                                "\""
                );
            }
            try {
                converter = converterType.getConstructor().newInstance();
            } catch (Exception ex) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", it cannot be decorated by @" +
                                annotationType.getName() +
                                ", cannot create instance for converter type \"" +
                                converterType.getName() +
                                "\"",
                        ex instanceof InvocationTargetException ?
                                ((InvocationTargetException)ex).getTargetException() :
                                ex
                );
            }
        }
        converterResolved = true;
        return converter;
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
        validateDeclaringEntity("storage");
        storage = Storages.of(this);
        storageResolved = true;
        if (!nullable &&
                storage instanceof ColumnDefinition &&
                isReference(TargetLevel.ENTITY) &&
                !((ColumnDefinition) storage).isForeignKey()
        ) {
            throw new ModelException(
                "Illegal reference association property \"" +
                        this +
                        "\", it is based on fake foreign key(`foreignKey` = false), " +
                        "so that it must be nullable"
            );
        }
        return (S)storage;
    }

    @Override
    public boolean isId() {
        return this == declaringType.getIdProp() || (base != null && base.isId());
    }

    @Override
    public boolean isVersion() {
        return this == declaringType.getVersionProp() || (base != null && base.isVersion());
    }

    @Override
    public boolean isLogicalDeleted() {
        LogicalDeletedInfo info = declaringType.getLogicalDeletedInfo();
        return info != null && info.getProp() == this || (base != null && base.isLogicalDeleted());
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
            if (isReferenceList(TargetLevel.PERSISTENT)) {
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
                    if (!prop.isScalar(TargetLevel.PERSISTENT)) {
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
            validateDeclaringEntity("mappedBy");
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
                if (resolved.isReferenceList(TargetLevel.PERSISTENT) &&
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
        if (isAssociation(TargetLevel.PERSISTENT)) {
            validateDeclaringEntity("opposite");
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
    public List<Dependency> getDependencies() {
        List<Dependency> list = dependencies;
        if (list == null) {
            return getDependenciesImpl(new LinkedList<>());
        }
        return list;
    }

    @Override
    public boolean isRemote() {
        Boolean remote = isRemote;
        if (remote == null) {
            if (isAssociation(TargetLevel.ENTITY)) {
                remote = !declaringType.getMicroServiceName().equals(getTargetType().getMicroServiceName());
            } else {
                remote = false;
            }
            isRemote = remote;
        }
        return remote;
    }

    private List<Dependency> getDependenciesImpl(LinkedList<ImmutableProp> stack) {
        List<Dependency> list = dependencies;
        if (list == null) {
            list = new ArrayList<>();
            Formula formula = getAnnotation(Formula.class);
            ImmutableProp idViewBaseProp = getIdViewBaseProp();
            ImmutableProp manyToManyToBaseProp = getManyToManyViewBaseProp();
            if (formula != null) {
                String[] arr = formula.dependencies();
                if (arr.length != 0) {
                    Map<String, ImmutableProp> propMap = declaringType.getProps();
                    stack.push(this);
                    try {
                        for (String name : arr) {
                            ImmutableProp prop = propMap.get(name);
                            if (prop == null) {
                                throw new ModelException(
                                        "Illegal property \"" +
                                                this +
                                                "\", its dependency property \"" +
                                                declaringType +
                                                '.' +
                                                name +
                                                "\" does not exists"
                                );
                            }
                            if (stack.contains(prop)) {
                                throw new ModelException(
                                        "Illegal entity type \"" +
                                                declaringType +
                                                "\", dependency cycle has been found: " +
                                                stack
                                );
                            }
                            boolean isValid = prop.isFormula() || (
                                    prop.getStorage() != null && !prop.isReference(TargetLevel.PERSISTENT)
                            );
                            if (!isValid) {
                                throw new ModelException(
                                        "Illegal property \"" +
                                                this +
                                                "\", its dependency property \"" +
                                                prop +
                                                "\" must be scalar property or another formula property"
                                );
                            }
                            if (prop.isFormula()) {
                                if (prop.getFormulaTemplate() != null) {
                                    throw new ModelException(
                                            "Illegal property \"" +
                                                    this +
                                                    "\", it is an abstract formula property based on SQL exception but " +
                                                    "its dependency property \"" +
                                                    prop +
                                                    "\" is another formula property" +
                                                    "(This is only allowed for non-abstract formula property " +
                                                    "based on java/kotlin expression)"
                                    );
                                }
                                // Deeper check to find dependency cycle
                                ((ImmutablePropImpl)prop).getDependenciesImpl(stack);
                            }
                            list.add(new Dependency(prop));
                        }
                    } finally {
                        stack.pop();
                    }
                }
            } else if (getIdViewBaseProp() != null) {
                list.add(new Dependency(getIdViewBaseProp()));
            } else if (getManyToManyViewBaseProp() != null) {
                list.add(new Dependency(getManyToManyViewBaseProp(), getManyToManyViewBaseDeeperProp()));
            }
            dependencies = Collections.unmodifiableList(list);
        }
        return list;
    }

    ImmutableProp getBase() {
        return base;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(base != null ? base : this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImmutablePropImpl)) {
            return false;
        }
        ImmutablePropImpl prop = (ImmutablePropImpl) o;
        return (base != null ? base : this) == (prop.base != null ? prop.base : prop);
    }

    @Override
    public String toString() {
        return declaringType.toString() + '.' + name;
    }

    private void validateDeclaringEntity(String value) {
        if (!this.declaringType.isEntity()) {
            throw new UnsupportedOperationException(
                    "Cannot get the `" + value + "` of \"" + this + "\" because it is not declared in entity"
            );
        }
    }
}
