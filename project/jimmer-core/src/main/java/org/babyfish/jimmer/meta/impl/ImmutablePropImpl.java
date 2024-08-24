package org.babyfish.jimmer.meta.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;
import kotlin.reflect.full.KClasses;
import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.Scalar;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.JacksonUtils;
import org.babyfish.jimmer.jackson.ConverterMetadata;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.meta.spi.ImmutablePropImplementor;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.meta.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

class ImmutablePropImpl implements ImmutableProp, ImmutablePropImplementor {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    private static final LogicalDeletedValueGenerator<?> NIL_LOGICAL_DELETED_VALUE_GENERATOR =
            new LogicalDeletedValueGenerator<Object>() {
                @Override
                public Object generate() {
                    throw new UnsupportedOperationException();
                }
            };

    private static Lock META_LOCK = new ReentrantLock();

    private static final Ref<Object> NIL_REF = Ref.of(null);

    private final ImmutableTypeImpl declaringType;

    private final PropId id;

    private final String name;

    private final ImmutablePropCategory category;

    private final Class<?> elementClass;

    private final boolean nullable;

    private final boolean inputNotNull;

    private final KProperty1<?, ?> kotlinProp;

    private final Method javaGetter;

    private final Annotation associationAnnotation;

    private final Class<? extends Annotation> primaryAnnotationType;

    private final TargetTransferMode targetTransferMode;

    private final boolean isTransient;

    private final boolean hasTransientResolver;

    private final boolean isFormula;

    private final SqlTemplate sqlTemplate;

    private final DissociateAction dissociateAction;

    private final ImmutablePropImpl original;

    private ConverterMetadata converterMetadata;

    private boolean converterMetadataResolved;

    private int storageType; // 1: NONE, 2: COLUMN_DEFINITION, 3: MIDDLE_TABLE

    private ImmutableTypeImpl targetType;

    private boolean targetTypeResolved;

    private List<OrderedItem> orderedItems;

    private ImmutableProp mappedBy;

    private ImmutableProp acceptedMappedBy;

    private boolean mappedByResolved;

    private ImmutableProp opposite;

    private boolean oppositeResolved;

    private List<Dependency> dependencies;

    private List<ImmutableProp> propsDependOnSelf;

    private Ref<Object> defaultValueRef;

    private Boolean isExcludedFromAllScalarsRef;

    private ImmutableProp idViewProp;

    private boolean idViewPropResolved;

    private ImmutableProp idViewBaseProp;

    private boolean idViewBasePropResolved;

    private ImmutableProp manyToManyViewBaseProp;

    private ImmutableProp manyToManyViewBaseDeeperProp;

    private boolean manyToManyViewBasePropResolved;

    private Boolean isRemote;

    private final MetaCache<Storage> storageCache =
            new MetaCache<>(it -> Storages.of(this, it));

    private final MetaCache<Boolean> isTargetForeignKeyRealCache =
            new MetaCache<>(this::isTargetForeignKeyReal0);

    private final SqlContextCache<LogicalDeletedValueGenerator<?>> logicalDeletedValueGeneratorCache = new SqlContextCache<>(it -> {
        ImmutableProp prop = getMappedBy() != null ? getMappedBy() : this;
        Storage storage = prop.getStorage(it.getMetadataStrategy());
        if (storage instanceof MiddleTable) {
            LogicalDeletedValueGenerator<?> g = LogicalDeletedValueGenerators.of(
                    ((MiddleTable) storage).getLogicalDeletedInfo(),
                    it
            );
            return g != null ? g : NIL_LOGICAL_DELETED_VALUE_GENERATOR;
        }
        return NIL_LOGICAL_DELETED_VALUE_GENERATOR;
    });

    ImmutablePropImpl(
            ImmutableTypeImpl declaringType,
            PropId id,
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

        OneToMany oneToMany = getAnnotation(OneToMany.class);
        OneToOne oneToOne = getAnnotation(OneToOne.class);
        if (oneToMany != null) {
            this.targetTransferMode = oneToMany.targetTransferMode();
        } else if (oneToOne != null) {
            this.targetTransferMode = oneToOne.targetTransferMode();
            if (this.targetTransferMode != TargetTransferMode.AUTO && oneToOne.mappedBy().isEmpty()) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", `targetTransferMode` can only be specified when `mappedBy` is specified too"
                );
            }
        } else {
            this.targetTransferMode = TargetTransferMode.AUTO;
        }

        Transient trans = getAnnotation(Transient.class);
        isTransient = trans != null;
        hasTransientResolver = trans != null && (
                trans.value() != void.class || !trans.ref().isEmpty()
        );

        Formula formula = getAnnotation(Formula.class);
        if (declaringType.isEmbeddable() && formula != null && !formula.sql().isEmpty()) {
            throw new ModelException(
                    "Illegal property \"" +
                            this +
                            "\", The sql based formula property cannot be declared in embeddable type"
            );
        }
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

        JoinSql joinSql = getAnnotation(JoinSql.class);
        if (formula != null && !formula.sql().isEmpty()) {
            try {
                sqlTemplate = FormulaTemplate.of(formula.sql());
            } catch (IllegalArgumentException ex) {
                throw new ModelException(
                        "Illegal property \"" +
                                this +
                                "\", the formula sql template: " +
                                ex.getMessage()
                );
            }
        } else if (joinSql != null) {
            sqlTemplate = JoinTemplate.of(joinSql.value());
        } else {
            sqlTemplate = null;
        }

        ManyToOne manyToOne = getAnnotation(ManyToOne.class);
        inputNotNull = manyToOne != null ?
                manyToOne.inputNotNull() :
                oneToOne != null && oneToOne.inputNotNull();
        if (isInputNotNull() && !nullable) {
            throw new ModelException(
                    "Illegal property \"" +
                            this +
                            "\", the `inputNotNull` cannot be specified for non-null property"
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

        if (associationType != null) {
            associationAnnotation = getAnnotation(associationType);
            primaryAnnotationType = associationType;
        } else {
            associationAnnotation = null;
            if (isId()) {
                primaryAnnotationType = Id.class;
            } else if (isVersion()) {
                primaryAnnotationType = Version.class;
            } else if (isLogicalDeleted()) {
                primaryAnnotationType = LogicalDeleted.class;
            } else if (isFormula) {
                primaryAnnotationType = Formula.class;
            } else if (isTransient) {
                primaryAnnotationType = Transient.class;
            } else if (getAnnotation(IdView.class) != null) {
                primaryAnnotationType = IdView.class;
            } else if (getAnnotation(ManyToManyView.class) != null) {
                primaryAnnotationType = ManyToManyView.class;
            } else {
                primaryAnnotationType = Scalar.class;
            }
        }

        this.original = null;
    }

    ImmutablePropImpl(
            ImmutablePropImpl original,
            ImmutableTypeImpl declaringType,
            PropId id
    ) {
        if (!original.getDeclaringType().isAssignableFrom(declaringType)) {
            throw new IllegalArgumentException(
                    "The new declaring type \"" +
                            declaringType +
                            "\" is illegal, it is not derived type of original declaring type \"" +
                            original.getDeclaringType() +
                            "\""
            );
        }
        while (original.original != null) {
            original = original.original;
        }
        this.declaringType = declaringType;
        this.id = id != null ? id: original.id;
        this.name = original.name;
        this.category = original.category;
        this.elementClass = original.elementClass;
        this.nullable = original.nullable;
        this.inputNotNull = original.inputNotNull;
        this.kotlinProp = original.kotlinProp;
        this.javaGetter = original.javaGetter;
        this.associationAnnotation = original.associationAnnotation;
        this.primaryAnnotationType = original.primaryAnnotationType;
        this.targetTransferMode = original.targetTransferMode;
        this.isTransient = original.isTransient;
        this.isFormula = original.isFormula;
        this.sqlTemplate = original.sqlTemplate;
        this.hasTransientResolver = original.hasTransientResolver;
        this.dissociateAction = original.dissociateAction;
        this.original = original;
    }

    @NotNull
    @Override
    public ImmutableType getDeclaringType() {
        return declaringType;
    }

    @Override
    public PropId getId() {
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

    @NotNull
    @Override
    public Class<?> getReturnClass() {
        return javaGetter.getReturnType();
    }

    @NotNull
    @Override
    public Type getGenericType() {
        return javaGetter.getGenericReturnType();
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
        return !isFormula || sqlTemplate instanceof FormulaTemplate;
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
    public Class<? extends Annotation> getPrimaryAnnotationType() {
        return primaryAnnotationType;
    }

    @Override
    public TargetTransferMode getTargetTransferMode() {
        return targetTransferMode;
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

    @Override
    public boolean isTargetForeignKeyReal(MetadataStrategy strategy) {
        return isTargetForeignKeyRealCache.get(strategy);
    }

    private boolean isTargetForeignKeyReal0(MetadataStrategy strategy) {
        if (!isAssociation(TargetLevel.PERSISTENT)) {
            return false;
        }
        Storage storage = getStorage(strategy);
        if (storage == null) {
            ImmutableProp mappedBy = getMappedBy();
            if (mappedBy != null && !isRemote()) {
                storage = mappedBy.getStorage(strategy);
                if (storage instanceof MiddleTable) {
                    return ((MiddleTable)storage).getColumnDefinition().isForeignKey();
                }
            }
            return false;
        }
        if (storage instanceof MiddleTable) {
            return ((MiddleTable)storage).getTargetColumnDefinition().isForeignKey();
        }
        if (storage instanceof ColumnDefinition) {
            return ((ColumnDefinition)storage).isForeignKey();
        }
        return false;
    }

    @Nullable
    @Override
    public SqlTemplate getSqlTemplate() {
        return sqlTemplate;
    }

    @Override
    public boolean isView() {
        return idViewBaseProp != null || manyToManyViewBaseProp != null;
    }

    @Override
    public ImmutableProp getIdViewProp() {
        if (idViewPropResolved) {
            return idViewProp;
        }
        if (isAssociation(TargetLevel.ENTITY)) {
            for (ImmutableProp otherProp : declaringType.getProps().values()) {
                if (otherProp.getIdViewBaseProp() == this) {
                    idViewProp = otherProp;
                    break;
                }
            }
        }
        idViewPropResolved = true;
        return idViewProp;
    }

    @Override
    public ImmutableProp getIdViewBaseProp() {
        if (idViewBasePropResolved) {
            return idViewBaseProp;
        }
        META_LOCK.lock();
        try {
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
                    basePropName = Utils.defaultViewBasePropName(isReferenceList(TargetLevel.OBJECT) || isScalarList(), name);
                    if (basePropName == null) {
                        throw new ModelException(
                                "Illegal property \"" +
                                        this +
                                        "\", it is decorated by \"" +
                                        IdView.class.getName() +
                                        "\" but the base property name cannot be determined automatically"
                        );
                    }
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
        } finally {
            META_LOCK.unlock();
        }
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
    public ConverterMetadata getConverterMetadata() {
        if (converterMetadataResolved) {
            return converterMetadata;
        }
        ConverterMetadata metadata = null;
        if (getIdViewBaseProp() != null) {
            metadata = getIdViewBaseProp().getTargetType().getIdProp().getConverterMetadata();
            if (metadata != null && getIdViewBaseProp().isReferenceList(TargetLevel.ENTITY)) {
                metadata = metadata.toListMetadata();
            }
        } else {
            JsonConverter jsonConverter = JacksonUtils.getAnnotation(this, JsonConverter.class);
            if (jsonConverter != null) {
                if (isAssociation(TargetLevel.OBJECT)) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it cannot be decorated by \"@" +
                                    JsonConverter.class +
                                    "\" because it is " +
                                    (isReferenceList(TargetLevel.OBJECT) ? "list" : "reference") +
                                    " of immutable object"
                    );
                }
                if (JacksonUtils.getAnnotation(this, JsonFormat.class) != null) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it cannot be decorated by both \"@" +
                                    JsonConverter.class +
                                    "\" and \"@" +
                                    JsonFormat.class +
                                    "\""
                    );
                }
                metadata = ConverterMetadata.of(jsonConverter.value());
                Type genericReturnType = getJavaGetter().getGenericReturnType();
                if (genericReturnType instanceof Class<?>) {
                    Class<?> type = (Class<?>) genericReturnType;
                    if (type.isPrimitive()) {
                        genericReturnType = Classes.boxTypeOf(type);
                    }
                }
                if (!metadata.getSourceType().equals(genericReturnType)) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    this +
                                    "\", it cannot be decorated by @" +
                                    JsonConverter.class.getName() +
                                    ", the property type \"" +
                                    javaGetter.getGenericReturnType() +
                                    "\" does not match the source type \"" +
                                    metadata.getSourceType() +
                                    "\" of converter class \"" +
                                    metadata.getConverter().getClass().getName() +
                                    "\""
                    );
                }
            }
        }
        converterMetadata = metadata;
        converterMetadataResolved = true;
        return metadata;
    }

    @Nullable
    @Override
    public <S, T> Converter<S, T> getConverter() {
        return getConverter(false);
    }

    @Override
    public <S, T> Converter<S, T> getConverter(boolean forList) {
        ConverterMetadata metadata = getConverterMetadata();
        if (metadata == null) {
            return null;
        }
        if (forList) {
            metadata = metadata.toListMetadata();
        }
        return metadata.getConverter();
    }

    @Nullable
    @Override
    public <S, T> Converter<S, T> getAssociatedIdConverter(boolean forList) {
        ImmutableType target = getTargetType();
        if (target == null) {
            throw new IllegalStateException("The current property \"" + this + "\" is not association");
        }
        ConverterMetadata metadata = targetType.getIdProp().getConverterMetadata();
        if (metadata == null) {
            return null;
        }
        metadata = forList && isReferenceList(TargetLevel.ENTITY) ? metadata.toListMetadata() : metadata;
        return metadata.getConverter();
    }

    @NotNull
    @Override
    public DissociateAction getDissociateAction() {
        return dissociateAction;
    }

    @Override
    public boolean hasStorage() {
        return getStorageType() > 1;
    }

    @Override
    public boolean isColumnDefinition() {
        return getStorageType() == 2;
    }

    @Override
    public boolean isMiddleTableDefinition() {
        return getStorageType() == 3;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Storage> S getStorage(MetadataStrategy strategy) {
        if (getStorageType() <= 1) {
            return null;
        }
        return (S)storageCache.get(strategy);
    }

    private int getStorageType() {
        int type = storageType;
        if (type == 0) {
            META_LOCK.lock();
            try {
                type = storageType;
                if (type == 0) {
                    int result;
                    if (isTransient() ||
                            isFormula() ||
                            !getDependencies().isEmpty() ||
                            getSqlTemplate() instanceof JoinTemplate ||
                            getMappedBy() != null
                    ) {
                        result = 1;
                    } else if (!(associationAnnotation instanceof ManyToMany) && getAnnotation(JoinTable.class) == null) {
                        result = 2;
                    } else {
                        result = 3;
                    }
                    storageType = type = result;
                }
            } finally {
                META_LOCK.unlock();
            }
        }
        return type;
    }

    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(SqlContext sqlContext) {
        LogicalDeletedValueGenerator<?> generator = logicalDeletedValueGeneratorCache.get(sqlContext);
        return generator == NIL_LOGICAL_DELETED_VALUE_GENERATOR ? null : generator;
    }

    @Override
    public boolean isId() {
        return this == declaringType.getIdProp() || (original != null && original.isId());
    }

    @Override
    public boolean isVersion() {
        return this == declaringType.getVersionProp() || (original != null && original.isVersion());
    }

    @Override
    public boolean isLogicalDeleted() {
        LogicalDeletedInfo info = declaringType.getLogicalDeletedInfo();
        return info != null && info.getProp() == this || (original != null && original.isLogicalDeleted());
    }

    @Override
    public ImmutableType getTargetType() {
        if (targetTypeResolved) {
            return targetType;
        }
        META_LOCK.lock();
        try {
            if (targetTypeResolved) {
                return targetType;
            }
            if (isAssociation(TargetLevel.OBJECT)) {
                targetType = (ImmutableTypeImpl) Metadata.tryGet(elementClass);
                if (targetType == null) {
                    throw new ModelException(
                            "Cannot resolve target type of \"" +
                                    this +
                                    "\""
                    );
                }
                if (sqlTemplate != null &&
                        declaringType.isEntity() &&
                        !declaringType.getMicroServiceName().equals(targetType.getMicroServiceName())) {
                    throw new ModelException(
                            "Illegal association property \"" +
                                    this +
                                    "\", it is is remote association so that it cannot be decorated by \"@" +
                                    JoinSql.class.getName() +
                                    "\""
                    );
                }
            }
            targetTypeResolved = true;
            return targetType;
        } finally {
            META_LOCK.unlock();
        }
    }

    @Override
    public List<OrderedItem> getOrderedItems() {
        List<OrderedItem> orderedItems = this.orderedItems;
        if (orderedItems == null) {
            META_LOCK.lock();
            try {
                orderedItems = this.orderedItems;
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
                            ImmutableProp prop = targetType.getProps().get(orderedProp.value());
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
            } finally {
                META_LOCK.unlock();
            }
        }
        return orderedItems;
    }

    @Override
    public ImmutableProp getMappedBy() {
        if (mappedByResolved) {
            return mappedBy;
        }
        META_LOCK.lock();
        try {
            if (mappedByResolved) {
                return mappedBy;
            }
            if (isAssociation(TargetLevel.ENTITY)) {
                String mappedBy = getMappedByValue();
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
                    if (!resolved.hasStorage() && !(resolved.getSqlTemplate() instanceof JoinTemplate)) {
                        throw new ModelException(
                                "The property \"" +
                                        resolved +
                                        "\" is illegal, it's not persistence property so that " +
                                        "\"" +
                                        this +
                                        "\" cannot reference it by \"mappedBy\""
                        );
                    }
                    Annotation resolvedAssociationAnnotation = resolved.getAssociationAnnotation();
                    if (resolvedAssociationAnnotation == null) {
                        throw new ModelException(
                                "Illegal property \"" +
                                        this +
                                        "\", " +
                                        "because its \"mappedBy\" property \"" +
                                        resolved +
                                        "\" is not association property"
                        );
                    }
                    if (resolvedAssociationAnnotation.annotationType() == OneToOne.class &&
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
                    if (resolvedAssociationAnnotation.annotationType() == ManyToOne.class &&
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
                    ((ImmutablePropImpl) resolved).acceptMappedBy(this);
                    this.mappedBy = resolved;
                }
            }
            mappedByResolved = true;
            return mappedBy;
        } finally {
            META_LOCK.unlock();
        }
    }

    String getMappedByValue() {
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
        META_LOCK.lock();
        try {
            if (oppositeResolved) {
                return opposite;
            }
            if (isAssociation(TargetLevel.PERSISTENT)) {
                if (!declaringType.isEntity()) {
                    throw new UnsupportedOperationException(
                            "Cannot access the `opposite` of \"" +
                                    this +
                                    "\" because it is not declared in entity"
                    );
                }
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
        } finally {
            META_LOCK.unlock();
        }
    }

    @Override
    public ImmutableProp getReal() {
        ImmutableProp mappedBy = getMappedBy();
        return mappedBy != null ? mappedBy : this;
    }

    @Override
    public List<Dependency> getDependencies() {
        List<Dependency> list = dependencies;
        if (list == null) {
            META_LOCK.lock();
            try {
                list = dependencies;
                if (list == null) {
                    dependencies = list = Collections.unmodifiableList(getDependenciesImpl(new LinkedList<>()));
                }
            } finally {
                META_LOCK.unlock();
            }
        }
        return list;
    }

    @Override
    public List<ImmutableProp> getPropsDependOnSelf() {
        List<ImmutableProp> list = propsDependOnSelf;
        if (list == null) {
            META_LOCK.lock();
            try {
                list = propsDependOnSelf;
                if (list == null) {
                    list = new ArrayList<>();
                    for (ImmutableProp prop : getDeclaringType().getProps().values()) {
                        if (prop != this && prop.getDependencies().stream().anyMatch(it -> it.getProps().get(0) == this)) {
                            list.add(prop);
                        }
                    }
                    propsDependOnSelf = Collections.unmodifiableList(list);
                }
            } finally {
                META_LOCK.unlock();
            }
        }
        return list;
    }

    @Override
    public Ref<Object> getDefaultValueRef() {
        Ref<Object> ref = this.defaultValueRef;
        if (ref == null) {
            META_LOCK.lock();
            try {
                ref = this.defaultValueRef;
                if (ref == null) {
                    Default dft = getAnnotation(Default.class);
                    if (dft == null || dft.value().isEmpty()) {
                        if (isLogicalDeleted()) {
                            LogicalDeletedInfo info = declaringType.getLogicalDeletedInfo();
                            assert info != null;
                            ref = Ref.of(info.allocateInitializedValue());
                        } else {
                            ref = NIL_REF;
                        }
                    } else {
                        if (isId()) {
                            throw new ModelException(
                                    "Illegal property \"" +
                                            this +
                                            "\", the id property cannot be decorated by \"@" +
                                            Default.class.getName() +
                                            "\""
                            );
                        }
                        if (isReferenceList(TargetLevel.ENTITY)) {
                            throw new ModelException(
                                    "Illegal property \"" +
                                            this +
                                            "\", the association property cannot be decorated by \"@" +
                                            Default.class.getName() +
                                            "\""
                            );
                        }
                        if (isEmbedded(EmbeddedLevel.BOTH)) {
                            throw new ModelException(
                                    "Illegal property \"" +
                                            this +
                                            "\", the embedded property cannot be decorated by \"@" +
                                            Default.class.getName() +
                                            "\""
                            );
                        }
                        if (isFormula()) {
                            throw new ModelException(
                                    "Illegal property \"" +
                                            this +
                                            "\", the formula property cannot be decorated by \"@" +
                                            Default.class.getName() +
                                            "\""
                            );
                        }
                        if (isTransient()) {
                            throw new ModelException(
                                    "Illegal property \"" +
                                            this +
                                            "\", the tranisent property cannot be decorated by \"@" +
                                            Default.class.getName() +
                                            "\""
                            );
                        }
                        if (getIdViewBaseProp() != null || getManyToManyViewBaseProp() != null) {
                            throw new ModelException(
                                    "Illegal property \"" +
                                            this +
                                            "\", the view property cannot be decorated by \"@" +
                                            Default.class.getName() +
                                            "\""
                            );
                        }
                        Object value = MetadataLiterals.valueOf(getGenericType(), isNullable(), dft.value());
                        ref = Ref.of(value);
                    }
                    this.defaultValueRef = ref;
                }
            } finally {
                META_LOCK.unlock();
            }
        }
        return ref == NIL_REF ? null : ref;
    }

    @Override
    public boolean isExcludedFromAllScalars() {
        Boolean ref = isExcludedFromAllScalarsRef;
        if (ref == null) {
            META_LOCK.lock();
            try {
                ref = isExcludedFromAllScalarsRef;
                if (ref == null) {
                    boolean isExecluded = false;
                    if (kotlinProp != null) {
                        isExecluded = kotlinProp.getAnnotations().stream().anyMatch(it ->
                                it.annotationType() == ExcludeFromAllScalars.class
                        );
                    }
                    if (!isExecluded) {
                        isExecluded = javaGetter.isAnnotationPresent(ExcludeFromAllScalars.class);
                    }
                    this.isExcludedFromAllScalarsRef = ref = isExecluded;
                }
            } finally {
                META_LOCK.unlock();
            }
        }
        return ref;
    }

    @Override
    public boolean isRemote() {
        Boolean remote = isRemote;
        if (remote == null) {
            META_LOCK.lock();
            try {
                remote = isRemote;
                if (remote == null) {
                    if (isAssociation(TargetLevel.ENTITY)) {
                        remote = !declaringType.getMicroServiceName().equals(getTargetType().getMicroServiceName());
                        if (remote && sqlTemplate != null) {
                            throw new ModelException(
                                    "Illegal property \"" +
                                            this +
                                            "\", remote association(micro-service names of declaring type and target type " +
                                            "are different) cannot be decorated by \"@" +
                                            JoinSql.class.getName() +
                                            "\""
                            );
                        }
                    } else {
                        remote = false;
                    }
                    isRemote = remote;
                }
            } finally {
                META_LOCK.unlock();
            }
        }
        return remote;
    }

    @Override
    public ImmutableProp toOriginal() {
        return original != null ? original : this;
    }

    private List<Dependency> getDependenciesImpl(LinkedList<ImmutableProp> stack) {
        List<Dependency> list = dependencies;
        if (list == null) {
            META_LOCK.lock();
            try {
                list = dependencies;
                if (list == null) {
                    list = new ArrayList<>();
                    Formula formula = getAnnotation(Formula.class);
                    if (formula != null) {
                        String[] arr = formula.dependencies();
                        if (arr.length != 0) {
                            Map<String, ImmutableProp> propMap = declaringType.getProps();
                            stack.push(this);
                            try {
                                for (String dependency : arr) {
                                    list.add(createFormulaDependency(stack, this, dependency));
                                }
                            } finally {
                                stack.pop();
                            }
                        }
                    } else if (getIdViewBaseProp() != null) {
                        list.add(new Dependency(Collections.singletonList(getIdViewBaseProp())));
                    } else if (getManyToManyViewBaseProp() != null) {
                        list.add(new Dependency(getManyToManyViewBaseProp(), getManyToManyViewBaseDeeperProp()));
                    }
                }
            } finally {
                META_LOCK.unlock();
            }
        }
        return list;
    }

    ImmutableProp getOriginal() {
        return original;
    }

    @Override
    public int hashCode() {
        return declaringType.hashCode() ^ System.identityHashCode(original != null ? original : this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImmutablePropImpl)) {
            return false;
        }
        ImmutablePropImpl prop = (ImmutablePropImpl) o;
        return declaringType == prop.declaringType &&
                (original != null ? original : this) == (prop.original != null ? prop.original : prop);
    }

    @Override
    public String toString() {
        return declaringType.toString() + '.' + name;
    }

    private static Dependency createFormulaDependency(
            LinkedList<ImmutableProp> stack,
            ImmutableProp formulaProp,
            String dependency
    ) {
        String[] propNames = DOT_PATTERN.split(dependency);
        int len = propNames.length;
        List<ImmutableProp> props = new ArrayList<>(len);
        ImmutableType declaringType = formulaProp.getDeclaringType();
        for (int i = 0; i < len; i++) {
            String propName = propNames[i];
            ImmutableProp prop = declaringType.getProps().get(propName);
            if (prop == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                formulaProp +
                                "\", its dependency \"" +
                                dependency +
                                "\" cannot be resolved because there is no property \"" +
                                propName +
                                "\" in the type \"" +
                                declaringType +
                                "\""
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
            ImmutableType targetType = prop.getTargetType();
            if (i + 1 == len) {
                boolean isValid = prop.isFormula() ||
                        len > 1 ||
                        prop.hasStorage() ||
                        prop.isReferenceList(TargetLevel.PERSISTENT);
                if (!isValid) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    formulaProp +
                                    "\", its dependency property \"" +
                                    prop +
                                    "\" must be column-mapped property or another formula property"
                    );
                }
            } else if (targetType == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                formulaProp +
                                "\", its dependency \"" +
                                dependency +
                                "\" cannot be resolved because the property \"" +
                                prop +
                                "\" is not last property but it is neither association nor embedded property"
                );
            }
            if (prop.isFormula()) {
                if (prop.getSqlTemplate() instanceof FormulaTemplate) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    formulaProp +
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
            props.add(prop);
            declaringType = targetType;
        }
        return new Dependency(props);
    }
}
