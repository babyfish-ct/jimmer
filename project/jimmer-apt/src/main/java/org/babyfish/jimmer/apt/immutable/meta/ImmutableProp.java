package org.babyfish.jimmer.apt.immutable.meta;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.Scalar;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.apt.immutable.generator.Strings;
import org.babyfish.jimmer.apt.util.ConverterMetadata;
import org.babyfish.jimmer.apt.util.RecursiveAnnotations;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.impl.util.Keywords;
import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.meta.impl.Utils;
import org.babyfish.jimmer.meta.impl.PropDescriptor;
import org.babyfish.jimmer.sql.*;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;

public class ImmutableProp implements BaseProp {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private final Context context;

    private final ImmutableType declaringType;

    private final ExecutableElement executableElement;

    private final String name;

    private final int id;

    private final String slotName;

    private final String getterName;

    private final String setterName;

    private final String applierName;

    private final String adderByName;

    private final boolean beanStyle;

    private final String beanGetterName;

    private final String valueName;

    private final String loadedStateName;

    private final String deeperPropIdName;

    private final TypeMirror returnType;

    private final TypeName typeName;

    private final TypeName draftTypeName;

    private final TypeName elementTypeName;

    private final TypeName draftElementTypeName;

    private final TypeMirror elementType;

    private final boolean isTransient;

    private final boolean hasTransientResolver;

    private final boolean isFormula;

    private final boolean isJavaFormula;

    private final boolean isList;

    private final boolean isAssociation;

    private final boolean isEntityAssociation;

    private final boolean isNullable;

    private final boolean isReverse;

    private final boolean isId;

    private final boolean isKey;

    private final Map<ClassName, String> validationMessageMap;

    private Annotation associationAnnotation;

    private ImmutableType targetType;

    private boolean targetTypeResolved;

    private Set<FormulaDependency> _dependencies;

    private boolean dependenciesResolved;

    private ImmutableProp _idViewProp;

    private boolean idViewPropResolved;

    private ImmutableProp _idViewBaseProp;

    private boolean idViewBasePropResolved;

    private ImmutableProp _manyToManyViewBaseProp;

    private boolean manyToManyViewBasePropResolved;

    private boolean _isBaseProp;

    private boolean isBasePropResolved;

    private Boolean remote;

    private ConverterMetadata converterMetadata;

    private boolean converterMetadataResolved;

    public ImmutableProp(
            Context context,
            ImmutableType declaringType,
            ExecutableElement executableElement,
            int id
    ) {
        this.context = context;
        this.id = id;
        this.declaringType = declaringType;
        this.executableElement = executableElement;
        getterName = executableElement.getSimpleName().toString();
        returnType = executableElement.getReturnType();
        if (returnType.getKind() == TypeKind.VOID) {
            throw new MetaException(executableElement, "it cannot return void");
        }
        if (!executableElement.getParameters().isEmpty()) {
            throw new MetaException(executableElement, "it cannot have parameter(s)");
        }
        if (getterName.startsWith("is") &&
                getterName.length() > 2 &&
                Character.isUpperCase(getterName.charAt(2))) {
            Element returnedElement = context.getTypes().asElement(returnType);
            String returnedTypeName = returnedElement != null ? returnedElement.toString() : null;
            if (returnType.getKind() != TypeKind.BOOLEAN &&
                    !"java.lang.Boolean".equals(returnedTypeName)
            ) {
                throw new MetaException(
                        executableElement,
                        "the method whose name starts with \"is\" return returns boolean type"
                );
            }
        }

        if (!context.keepIsPrefix() &&
                returnType.getKind() == TypeKind.BOOLEAN &&
                getterName.startsWith("is") &&
                getterName.length() > 2 &&
                Character.isUpperCase(getterName.charAt(2))) {
            name =
                    getterName.substring(2, 3).toLowerCase() +
                            getterName.substring(3);
            setterName = "set" + getterName.substring(2);
            applierName = "apply" + getterName.substring(2);
            adderByName = "addInto" + getterName.substring(2);
            beanGetterName = getterName;
            beanStyle = true;
        } else if (getterName.startsWith("get") &&
                getterName.length() > 3 &&
                Character.isUpperCase(getterName.charAt(3))) {
            name =
                    getterName.substring(3, 4).toLowerCase() +
                            getterName.substring(4);
            setterName = "set" + getterName.substring(3);
            applierName = "apply" + getterName.substring(3);
            adderByName = "addInto" + getterName.substring(3);
            beanStyle = true;
            beanGetterName = getterName;
        } else {
            name = getterName;
            String suffix =
                    Character.toUpperCase(getterName.charAt(0)) +
                            getterName.substring(1);
            setterName = "set" + suffix;
            applierName = "apply" + suffix;
            adderByName = "addInto" + suffix;
            beanStyle = false;
            beanGetterName = (returnType.getKind() == TypeKind.BOOLEAN ? "is" : "get") + suffix;
        }
        if (Keywords.ILLEGAL_PROP_NAMES.contains(name)) {
            throw new MetaException(
                    executableElement,
                    "Illegal property \"" + name + "\" which is jimmer keyword"
            );
        }

        slotName = "SLOT_" + Strings.upper(name);
        valueName = "__" + name + "Value";
        loadedStateName = "__" + name + "Loaded";
        if (executableElement.getAnnotation(ManyToManyView.class) != null) {
            deeperPropIdName = "DEEPER_PROP_ID_" + Strings.upper(name);
        } else {
            deeperPropIdName = null;
        }

        if (executableElement.getAnnotation(Default.class) != null &&
                executableElement.getAnnotation(LogicalDeleted.class) != null) {
            boolean isValid = executableElement.getReturnType().getKind() == TypeKind.INT;
            if (!isValid) {
                if (executableElement.getReturnType().getKind() == TypeKind.DECLARED) {
                    DeclaredType declaredType = (DeclaredType) executableElement.getReturnType();
                    TypeElement typeElement = (TypeElement) declaredType.asElement();
                    TypeElement superTypeElement = (TypeElement) ((DeclaredType)typeElement.getSuperclass()).asElement();
                    isValid = superTypeElement.getQualifiedName().toString().equals("java.lang.Enum");
                }
            }
            if (!isValid) {
                throw new MetaException(
                        executableElement,
                        "property cannot be decorated by both \"@Default\" and \"@LogicalDeleted\" " +
                                "unless its type is int or enum"
                );
            }
        }

        Formula formula = executableElement.getAnnotation(Formula.class);
        isFormula = formula != null;
        isJavaFormula = formula != null && formula.sql().isEmpty();

        if (context.isCollection(returnType) && !isExplicitScalar() && !isJavaFormula()) {
            if (!context.isListStrictly(returnType)) {
                throw new MetaException(
                        executableElement,
                        "the collection property which is not java formula property must return 'java.util.List'"
                );
            }
            List<? extends TypeMirror> typeArguments = ((DeclaredType)returnType).getTypeArguments();
            if (typeArguments.isEmpty()) {
                throw new MetaException(
                        executableElement,
                        "its return type must be generic type"
                );
            }
            isList = true;
            elementType = typeArguments.get(0);
            boolean isElementTypeValid = false;
            if (elementType.getKind().isPrimitive()) {
                isElementTypeValid = true;
            } else if (elementType instanceof DeclaredType) {
                isElementTypeValid = ((DeclaredType)elementType).getTypeArguments().isEmpty();
            }
            if (!isElementTypeValid) {
                throw new MetaException(
                        executableElement,
                        "its list whose elements are neither primitive type nor class/interface without generic parameters, " +
                                "whether to forcibly treat the current property as a non-list property " +
                                "(such as a JSON serialized field)?. if so, please decorate the current property with @" +
                                Scalar.class.getName() +
                                " or any other scalar-decorated annotations (such as @" +
                                Serialized.class +
                                ")"
                );
            }
        } else {
            isList = false;
            elementType = returnType;
        }

        if (context.isMappedSuperclass(elementType)) {
            throw new MetaException(
                    executableElement,
                    "the target type \"" +
                            TypeName.get(elementType) +
                            "\" is illegal, it cannot be type decorated by @MappedSuperclass"
            );
        }

        //------------------

        Transient trans = executableElement.getAnnotation(Transient.class);
        isTransient = trans != null;
        boolean hasResolver = false;
        if (isTransient) {
            for (AnnotationMirror mirror : executableElement.getAnnotationMirrors()) {
                if (((TypeElement) mirror.getAnnotationType().asElement())
                        .getQualifiedName()
                        .toString()
                        .equals(Transient.class.getName())) {
                    boolean hasValue = false;
                    boolean hasRef = false;
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues().entrySet()) {
                        if (e.getKey().getSimpleName().contentEquals("value")) {
                            hasValue = !e.getValue().toString().equals("void");
                        } else if (e.getKey().getSimpleName().contentEquals("ref")) {
                            hasRef = !e.getValue().toString().isEmpty();
                        }
                    }
                    if (hasValue && hasRef) {
                        throw new MetaException(
                                executableElement,
                                "it is decorated by @Transient, " +
                                        "the `value` and `ref` are both specified, this is not allowed"
                        );
                    }
                    hasResolver = hasValue || hasRef;
                }
            }
        }
        hasTransientResolver = hasResolver;

        isAssociation = context.isImmutable(elementType);
        if (declaringType.isAcrossMicroServices() && isAssociation && context.isEntity(elementType) && !isTransient) {
            throw new MetaException(
                    executableElement,
                    "association property is not allowed here " +
                            "because the declaring type is decorated by \"@" +
                            MappedSuperclass.class.getName() +
                            "\" with the argument `acrossMicroServices`"
            );
        }
        isEntityAssociation = context.isEntity(elementType);
        if (isList && context.isEmbeddable(elementType)) {
            throw new MetaException(
                    executableElement,
                    "the target type \"" +
                            TypeName.get(elementType) +
                            "\" is embeddable so that the property type cannot be list"
            );
        }

        elementTypeName = TypeName.get(elementType);
        if (isList) {
            typeName = ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    elementTypeName
            );
        } else {
            typeName = elementTypeName;
        }

        PropDescriptor.Builder builder = PropDescriptor.newBuilder(
                false,
                declaringType.getTypeElement().getQualifiedName().toString(),
                context.getImmutableAnnotationType(declaringType.getTypeElement()),
                this.toString(),
                ClassName.get(elementType).toString(),
                context.getImmutableAnnotationType(elementType),
                isList,
                typeName.isPrimitive() || typeName.isBoxedPrimitive() ?
                        typeName.isBoxedPrimitive() :
                        null,
                reason -> new MetaException(executableElement, reason)
        );
        List<AnnotationMirror> annotationMirrors = new ArrayList<>();
        annotationMirrors.addAll(executableElement.getAnnotationMirrors());
        annotationMirrors.addAll(executableElement.getReturnType().getAnnotationMirrors());
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            String annotationTypeName = ((TypeElement) annotationMirror.getAnnotationType().asElement())
                    .getQualifiedName()
                    .toString();
            builder.add(annotationTypeName);
            if (PropDescriptor.MAPPED_BY_PROVIDER_NAMES.contains(annotationTypeName)) {
                for (ExecutableElement key : annotationMirror.getElementValues().keySet()) {
                    if (key.getSimpleName().contentEquals("mappedBy")) {
                        builder.hasMappedBy();
                        break;
                    }
                }
            }
        }
        PropDescriptor descriptor = builder.build();
        if (descriptor.getType().isAssociation()) {
            associationAnnotation = executableElement.getAnnotation(descriptor.getType().getAnnotationType());
        }
        isNullable = descriptor.isNullable();
        isId = descriptor.getType() == PropDescriptor.Type.ID;
        isKey = getAnnotations(Key.class) != null;

        if (isAssociation) {
            OneToOne oneToOne = getAnnotation(OneToOne.class);
            OneToMany oneToMany = getAnnotation(OneToMany.class);
            ManyToMany manyToMany = getAnnotation(ManyToMany.class);
            isReverse = (oneToOne != null && !oneToOne.mappedBy().isEmpty()) ||
                    (oneToMany != null && !oneToMany.mappedBy().isEmpty()) ||
                    (manyToMany != null && !manyToMany.mappedBy().isEmpty());
        } else {
            isReverse = false;
        }

        if (isAssociation) {
            draftElementTypeName = ClassName.get(
                    ((ClassName)elementTypeName).packageName(),
                    ((ClassName)elementTypeName).simpleName() + "Draft"
            );
        } else {
            draftElementTypeName = elementTypeName;
        }
        if (isList) {
            draftTypeName = ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    draftElementTypeName
            );
        } else {
            draftTypeName = draftElementTypeName;
        }
        this.validationMessageMap = ValidationMessages.parseMessageMap(executableElement);
    }

    public ImmutableType getDeclaringType() {
        return declaringType;
    }

    public int getId() {
        return id;
    }

    public String getSlotName() {
        return slotName;
    }

    public String getName() {
        return name;
    }

    public String getGetterName() { return getterName; }

    public String getSetterName() {
        return setterName;
    }

    public String getApplierName() {
        return applierName;
    }

    public String getAdderByName() {
        return adderByName;
    }

    public boolean isBeanStyle() { return beanStyle; }

    public String getBeanGetterName() {
        return beanGetterName;
    }

    public String getValueName() {
        return valueName;
    }

    public String getLoadedStateName() {
        return getLoadedStateName(false);
    }

    public String getLoadedStateName(boolean force) {
        if (!force && !isLoadedStateRequired()) {
            throw new IllegalStateException("The property \"" + this + "\" does not has loaded state");
        }
        return loadedStateName;
    }

    public String getDeeperPropIdName() {
        return deeperPropIdName;
    }

    public TypeMirror getReturnType() {
        return returnType;
    }

    public TypeMirror getElementType() {
        return elementType;
    }

    public TypeName getTypeName() {
        return typeName;
    }

    public TypeName getDraftTypeName(boolean autoCreate) {
        if (isList && !autoCreate) {
            return typeName;
        }
        return draftTypeName;
    }

    public TypeName getElementTypeName() {
        return elementTypeName;
    }

    public TypeName getRawElementTypeName() {
        return elementTypeName instanceof ParameterizedTypeName ?
                ((ParameterizedTypeName)elementTypeName).rawType :
                elementTypeName;
    }

    public TypeName getDraftElementTypeName() {
        return draftElementTypeName;
    }

    public ConverterMetadata getConverterMetadata() {
        if (converterMetadataResolved) {
            return converterMetadata;
        }
        converterMetadata = determineConverterMetadata();
        converterMetadataResolved = true;
        return converterMetadata;
    }

    private ConverterMetadata determineConverterMetadata() {
        AnnotationMirror jsonConverter = RecursiveAnnotations.of(executableElement, JsonConverter.class.getName());
        if (jsonConverter != null) {
            if (isEntityAssociation) {
                throw new MetaException(
                        executableElement,
                        "it cannot be decorated by \"@" +
                                JsonConverter.class.getName() +
                                "\" because it is association"
                );
            }
            if (RecursiveAnnotations.of(executableElement, Constants.JSON_FORMAT_CLASS_NAME.reflectionName()) != null) {
                throw new MetaException(
                        executableElement,
                        "it cannot be decorated by both \"@" +
                                JsonConverter.class.getName() +
                                "\" and \"@" +
                                "com.fasterxml.jackson.annotation.JsonFormat" +
                                "\""
                );
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : jsonConverter.getElementValues().entrySet()) {
                if (e.getKey().getSimpleName().contentEquals("value")) {
                    TypeElement converterElement = context.getElements().getTypeElement(e.getValue().getValue().toString());
                    ConverterMetadata metadata = ConverterMetadata.of(converterElement);
                    if (!metadata.getSourceTypeName().equals(getTypeName().box())) {
                        throw new MetaException(
                                executableElement,
                                "The source type of converter \"" +
                                        converterElement.getQualifiedName().toString() +
                                        "\" is \"" +
                                        metadata.getSourceTypeName() +
                                        "\" which is not the return type of property"
                        );
                    }
                    return metadata;
                }
            }
        }
        return null;
    }

    public TypeName getClientTypeName() {
        if (converterMetadata != null) {
            return converterMetadata.getTargetTypeName();
        }
        return getTypeName();
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

    public boolean isJavaFormula() {
        return isJavaFormula;
    }

    @Override
    public boolean isList() {
        return isList;
    }

    @Override
    public boolean isReference() {
        return !isList && isAssociation(false);
    }

    @Override
    public boolean isEmbedded() {
        ImmutableType targetType = getTargetType();
        return targetType != null && targetType.isEmbeddable();
    }

    @Override
    public boolean isLogicalDeleted() {
        return getAnnotation(LogicalDeleted.class) != null;
    }

    @Override
    public boolean isExcludedFromAllScalars() {
        return getAnnotation(ExcludeFromAllScalars.class) != null;
    }

    @Override
    public boolean isAssociation(boolean entityLevel) {
        return entityLevel ? isEntityAssociation : isAssociation;
    }

    public boolean isRecursive() {
        return declaringType.isEntity() &&
                getManyToManyViewBaseProp() == null &&
                !isRemote() &&
                context.isSubType(
                        elementType,
                        declaringType.getTypeElement().asType()
                );
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    public boolean isReverse() {
        return isReverse;
    }

    @Override
    public boolean isId() {
        return isId;
    }

    @Override
    public boolean isKey() {
        return isKey;
    }

    public boolean isValueRequired() {
        return getIdViewBaseProp() == null && getManyToManyViewBaseProp() == null && !isJavaFormula;
    }

    public boolean isLoadedStateRequired() {
        return getIdViewBaseProp() == null && !isJavaFormula && (isNullable || typeName.isPrimitive());
    }

    public boolean isDsl(boolean isTableEx) {
        if (getIdViewBaseProp() != null) {
            return false;
        }
        if (isJavaFormula || isTransient || (getIdViewBaseProp() != null && isList)) {
            return false;
        }
        if (isRemote() && isReverse) {
            return false;
        }
        if (isTableEx && !isAssociation(true)) {
            return false;
        }
        if (isRemote() && !isList && isTableEx) {
            return false;
        }
        if (isList && isAssociation(true)) {
            return isTableEx;
        }
        return true;
    }

    public boolean isBaseProp() {
        if (isBasePropResolved) {
            return _isBaseProp;
        }
        _isBaseProp = isBaseProp0();
        isBasePropResolved = true;
        return _isBaseProp;
    }

    private boolean isBaseProp0() {
        for (ImmutableProp otherProp : declaringType.getProps().values()) {
            for (FormulaDependency dependency : otherProp.getDependencies()) {
                if (dependency.getProps().contains(this)) {
                    return true;
                }
            }
            if (otherProp.getIdViewBaseProp() == this) {
                return true;
            }
            if (otherProp.getManyToManyViewBaseProp() == this) {
                return true;
            }
        }
        return false;
    }

    public Class<?> getBoxType() {
        switch (returnType.getKind()) {
            case BOOLEAN:
                return Boolean.class;
            case CHAR:
                return Character.class;
            case BYTE:
                return Byte.class;
            case SHORT:
                return Short.class;
            case INT:
                return Integer.class;
            case LONG:
                return Long.class;
            case FLOAT:
                return Float.class;
            case DOUBLE:
                return Double.class;
            default:
                return null;
        }
    }

    public ImmutableProp getBaseProp() {
        ImmutableProp idViewBaseProp = getIdViewBaseProp();
        return idViewBaseProp != null ? idViewBaseProp : getManyToManyViewBaseProp();
    }

    public boolean isRemote() {
        Boolean remote = this.remote;
        if (remote == null) {
            if (isAssociation) {
                Element targetElement = context.getTypes().asElement(getElementType());
                Entity targetAnno = targetElement != null ? targetElement.getAnnotation(Entity.class) : null;
                remote = targetAnno != null &&
                        !targetAnno.microServiceName().equals(declaringType.getMicroServiceName());
                if (remote && getAnnotation(JoinSql.class) != null) {
                    throw new MetaException(
                            executableElement,
                            "the remote association(micro-service names of declaring type and target type are different) " +
                                    "cannot be decorated by \"@" +
                                    JoinSql.class +
                                    "\""
                    );
                }
            } else {
                remote = false;
            }
            this.remote = remote;
        }
        return remote;
    }

    private boolean isExplicitScalar() {
        for (AnnotationMirror mirror : executableElement.getAnnotationMirrors()) {
            if (isExplicitScalar(mirror, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExplicitScalar(AnnotationMirror mirror, Set<String> handledQualifiedNames) {
        TypeElement element = (TypeElement)mirror.getAnnotationType().asElement();
        String qualifiedName = element.getQualifiedName().toString();
        if (!handledQualifiedNames.add(qualifiedName)) {
            return false;
        }
        if (qualifiedName.equals(Scalar.class.getName())) {
            return true;
        }
        for (AnnotationMirror deeperMirror : element.getAnnotationMirrors()) {
            if (isExplicitScalar(deeperMirror, handledQualifiedNames)) {
                return true;
            }
        }
        return false;
    }

    public ImmutableType getTargetType() {
        if (!targetTypeResolved) {
            if (isAssociation) {
                targetType = context.getImmutableType(elementType);
                if (
                        (targetType.isEntity() || targetType.isMappedSuperClass()) &&
                                isRemote() &&
                                (declaringType.getMicroServiceName().isEmpty() || targetType.getMicroServiceName().isEmpty())
                ) {
                    throw new MetaException(
                            executableElement,
                            "when the micro service name(`" +
                                    declaringType.getMicroServiceName() +
                                    "`) of source type(" +
                                    declaringType.getQualifiedName() +
                                    ") and " +
                                    "the micro service name(`" +
                                    targetType.getMicroServiceName() +
                                    "`) of target type(" +
                                    targetType.getQualifiedName() +
                                    ") are not equal, " +
                                    "neither of them must be empty"
                    );
                }
            }
            targetTypeResolved = true;
        }
        return targetType;
    }

    public Set<FormulaDependency> getDependencies() {
        if (!dependenciesResolved) {
            Formula formula = getAnnotation(Formula.class);
            if (formula == null || formula.dependencies().length == 0) {
                this._dependencies = Collections.emptySet();
            } else {
                Set<FormulaDependency> dependencies = new LinkedHashSet<>();
                for (String dependency : formula.dependencies()) {
                    dependencies.add(createFormulaDependency(this, dependency));
                }
                this._dependencies = Collections.unmodifiableSet(dependencies);
            }
            dependenciesResolved = true;
        }
        return this._dependencies;
    }

    public ImmutableProp getIdViewProp() {
        if (idViewPropResolved) {
            return _idViewProp;
        }
        for (ImmutableProp prop : declaringType.getProps().values()) {
            if (prop.getIdViewBaseProp() == this) {
                _idViewProp = prop;
                break;
            }
        }
        idViewPropResolved = true;
        return _idViewProp;
    }

    @Override
    public ImmutableProp getIdViewBaseProp() {
        if (idViewBasePropResolved) {
            return _idViewBaseProp;
        }
        IdView idView = getAnnotation(IdView.class);
        if (idView == null) {
            idViewBasePropResolved = true;
            return null;
        }
        String base = idView.value();
        if (base.isEmpty()) {
            base = Utils.defaultViewBasePropName(isList, name);
            if (base == null) {
                throw new MetaException(
                        executableElement,
                        "it is decorated by \"@" +
                                IdView.class.getName() +
                                "\", the argument of that annotation is not specified by " +
                                "the base property name cannot be determined automatically, " +
                                "please specify the argument of that annotation"
                );
            }
        }
        if (base.equals(name)) {
            throw new MetaException(
                    executableElement,
                    "it is decorated by \"@" +
                            IdView.class.getName() +
                            "\", the argument of that annotation cannot be equal to the current property name\"" +
                            name +
                            "\""
            );
        }
        ImmutableProp baseProp = declaringType.getProps().get(base);
        if (baseProp == null) {
            throw new MetaException(
                    executableElement,
                    "it is decorated by \"@" +
                            IdView.class.getName() +
                            "\" but there is no base property \"" +
                            base +
                            "\" in the declaring type"
            );
        }
        if (!baseProp.isAssociation(true) || baseProp.isTransient) {
            throw new MetaException(
                    executableElement,
                    "it is decorated by \"@" +
                            IdView.class.getName() +
                            "\" but the base property \"" +
                            baseProp +
                            "\" is not persistence association"
            );
        }
        if (isList != baseProp.isList) {
            throw new MetaException(
                    executableElement,
                    "it " +
                            (isList ? "is" : "is not") +
                            " list and decorated by \"@" +
                            IdView.class.getName() +
                            "\" but the base property \"" +
                            baseProp +
                            "\" " +
                            (baseProp.isList ? "is" : "is not") +
                            " list"
            );
        }
        if (isNullable != baseProp.isNullable) {
            throw new MetaException(
                    executableElement,
                    "it " +
                            (isNullable ? "is" : "is not") +
                            " nullable and decorated by \"@" +
                            IdView.class.getName() +
                            "\" but the base property \"" +
                            baseProp +
                            "\" " +
                            (baseProp.isNullable ? "is" : "is not") +
                            " nullable"
            );
        }
        if (!elementTypeName.box().equals(baseProp.getTargetType().getIdProp().getElementTypeName().box())) {
            throw new MetaException(
                    executableElement,
                    "it is decorated by \"@" +
                            IdView.class.getName() +
                            "\", the base property \"" +
                            baseProp +
                            "\" returns entity type whose id is \"" +
                            baseProp.getTargetType().getIdProp().getElementTypeName() +
                            "\", but the current property does not return that type"
            );
        }
        idViewBasePropResolved = true;
        return _idViewBaseProp = baseProp;
    }

    @Override
    public ImmutableProp getManyToManyViewBaseProp() {
        if (manyToManyViewBasePropResolved) {
            return _manyToManyViewBaseProp;
        }
        ManyToManyView manyToManyView = getAnnotation(ManyToManyView.class);
        if (manyToManyView == null) {
            manyToManyViewBasePropResolved = true;
            return null;
        }
        String propName = manyToManyView.prop();
        ImmutableProp baseProp = declaringType.getProps().get(propName);
        if (baseProp == null) {
            throw new MetaException(
                    executableElement,
                    "it is decorated by \"@" +
                            ManyToManyView.class.getName() +
                            "\" with `baseProp` is \"" +
                            propName +
                            "\", but there is no such property in the declaring type"
            );
        }
        if (baseProp.getAnnotation(OneToMany.class) == null) {
            throw new MetaException(
                    executableElement,
                    "it is decorated by \"@" +
                            ManyToManyView.class.getName() +
                            "\" whose `baseProp` is \"" +
                            baseProp +
                            "\", but that property is not an one-to-many association"
            );
        }
        manyToManyViewBasePropResolved = true;
        return _manyToManyViewBaseProp = baseProp;
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return executableElement.getAnnotation(annotationType);
    }

    public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
        return executableElement.getAnnotationsByType(annotationType);
    }

    public List<? extends AnnotationMirror> getAnnotations() {
        return executableElement.getAnnotationMirrors();
    }

    public Annotation getAssociationAnnotation() {
        return associationAnnotation;
    }

    public Map<ClassName, String> getValidationMessageMap() {
        return validationMessageMap;
    }

    public ExecutableElement toElement() {
        return executableElement;
    }

    public Context context() {
        return context;
    }

    @Override
    public String toString() {
        return declaringType.getTypeElement().getQualifiedName().toString() + '.' + name;
    }

    private static FormulaDependency createFormulaDependency(
            ImmutableProp formulaProp,
            String dependency
    ) {
        ImmutableType declaringType = formulaProp.getDeclaringType();
        String[] propNames = DOT_PATTERN.split(dependency);
        int len = propNames.length;
        List<ImmutableProp> props = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            String propName = propNames[i];
            ImmutableProp prop = declaringType.getProps().get(propName);
            if (prop == null) {
                throw new MetaException(
                        formulaProp.executableElement,
                        "The dependency \"" +
                                dependency +
                                "\" cannot be resolved because there is no property \"" +
                                propName +
                                "\" in \"" +
                                declaringType +
                                "\""
                );
            }
            props.add(prop);
            if (i + 1 < len) {
                ImmutableType targetType = prop.getTargetType();
                if (targetType == null) {
                    throw new MetaException(
                            formulaProp.executableElement,
                            "The dependency \"" +
                                    dependency +
                                    "\" cannot be resolved because \"" +
                                    prop +
                                    "\" is not last property but it is neither association nor embedded property"
                    );
                }
                declaringType = targetType;
            }
        }
        return new FormulaDependency(props);
    }
}
