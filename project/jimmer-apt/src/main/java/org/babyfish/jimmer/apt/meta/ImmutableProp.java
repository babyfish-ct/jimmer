package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.meta.impl.PropDescriptor;
import org.babyfish.jimmer.sql.*;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;

public class ImmutableProp {

    private final ImmutableType declaringType;

    private final ExecutableElement executableElement;

    private final String name;

    private final int id;

    private final String getterName;

    private final String setterName;

    private final String applierName;

    private final String adderByName;

    private final boolean beanStyle;

    private final String loadedStateName;

    private final String visibleName;

    private final TypeMirror returnType;

    private final TypeName typeName;

    private final TypeName draftTypeName;

    private final TypeName elementTypeName;

    private final TypeName draftElementTypeName;

    private final TypeMirror elementType;

    private final boolean isTransient;

    private final boolean hasTransientResolver;

    private final boolean isJavaFormula;

    private final boolean isList;

    private final boolean isAssociation;

    private final boolean isEntityAssociation;

    private final boolean isNullable;

    private final boolean isReverse;

    private final Map<ClassName, String> validationMessageMap;

    private Annotation associationAnnotation;

    private ImmutableType targetType;

    private Set<ImmutableProp> dependencies;

    private ImmutableProp idViewBaseProp;

    private boolean isVisibilityControllable;

    public ImmutableProp(
            TypeUtils typeUtils,
            ImmutableType declaringType,
            ExecutableElement executableElement,
            int id
    ) {
        this.id = id;
        this.declaringType = declaringType;
        this.executableElement = executableElement;
        getterName = executableElement.getSimpleName().toString();
        returnType = executableElement.getReturnType();
        if (returnType.getKind() == TypeKind.VOID) {
            throw new MetaException(executableElement, "it cannot return void");
        }
        if (!executableElement.getParameters().isEmpty()) {
            throw new MetaException(executableElement, "it cannot have paremeter(s)");
        }

        if (returnType.getKind() == TypeKind.BOOLEAN &&
                getterName.startsWith("is") &&
                getterName.length() > 2 &&
                Character.isUpperCase(getterName.charAt(2))) {
            name =
                    getterName.substring(2, 3).toLowerCase() +
                    getterName.substring(3);
            setterName = "set" + getterName.substring(2);
            applierName = "apply" + getterName.substring(2);
            adderByName = "addInto" + getterName.substring(2);
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
        } else {
            name = getterName;
            String suffix =
                    getterName.substring(0, 1).toUpperCase() +
                    getterName.substring(1);
            setterName = "set" + suffix;
            applierName = "apply" + suffix;
            adderByName = "addInto" + suffix;
            beanStyle = false;
        }

        loadedStateName = "__" + name + "Loaded";
        visibleName = "__" + name + "Visible";

        if (typeUtils.isCollection(returnType)) {
            if (!typeUtils.isListStrictly(returnType)) {
                throw new MetaException(
                        executableElement,
                        "the collection property must return 'java.util.List'"
                );
            }
            List<? extends TypeMirror> typeArguments = ((DeclaredType)returnType).getTypeArguments();
            if (typeArguments.isEmpty()) {
                throw new MetaException(
                        executableElement,
                        "Its return type must be generic type"
                );
            }
            isList = true;
            elementType = typeArguments.get(0);
        } else {
            isList = false;
            elementType = returnType;
        }

        if (typeUtils.isMappedSuperclass(elementType)) {
            throw new MetaException(
                    executableElement,
                    "the target type \"" +
                            TypeName.get(elementType) +
                            "\" is illegal, it cannot be type decorated by @MappedSuperclass"
            );
        }

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

        Formula formula = executableElement.getAnnotation(Formula.class);
        isJavaFormula = formula != null && formula.sql().isEmpty();

        isAssociation = typeUtils.isImmutable(elementType);
        if (declaringType.isAcrossMicroServices() && isAssociation && typeUtils.isEntity(elementType) && !isTransient) {
            throw new MetaException(
                    executableElement,
                    "association property is not allowed here " +
                            "because the declaring type is decorated by \"@" +
                            MappedSuperclass.class.getName() +
                            "\" with the argument `acrossMicroServices`"
            );
        }
        isEntityAssociation = typeUtils.isEntity(elementType);
        if (isList && typeUtils.isEmbeddable(elementType)) {
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
                typeUtils.getImmutableAnnotationType(declaringType.getTypeElement()),
                this.toString(),
                ClassName.get(elementType).toString(),
                typeUtils.getImmutableAnnotationType(elementType),
                isList,
                typeName.isPrimitive() || typeName.isBoxedPrimitive() ?
                    typeName.isBoxedPrimitive() :
                    null,
                reason -> new MetaException(executableElement, reason)
        );
        for (AnnotationMirror annotationMirror : executableElement.getAnnotationMirrors()) {
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

    public String getLoadedStateName() {
        return getLoadedStateName(false);
    }

    public String getVisibleName() {
        return visibleName;
    }

    public String getLoadedStateName(boolean force) {
        if (!force && !isLoadedStateRequired()) {
            throw new IllegalStateException("The property \"" + this + "\" does not has loaded state");
        }
        return loadedStateName;
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

    public boolean isTransient() {
        return isTransient;
    }

    public boolean hasTransientResolver() {
        return hasTransientResolver;
    }

    public boolean isJavaFormula() {
        return isJavaFormula;
    }

    public boolean isList() {
        return isList;
    }

    public boolean isAssociation(boolean entityLevel) {
        return entityLevel ? isEntityAssociation : isAssociation;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public boolean isReverse() {
        return isReverse;
    }

    public boolean isValueRequired() {
        return idViewBaseProp == null && !isJavaFormula;
    }

    public boolean isLoadedStateRequired() {
        return idViewBaseProp == null && !isJavaFormula && (isNullable || typeName.isPrimitive());
    }

    public boolean isDsl(boolean isTableEx) {
        if (idViewBaseProp != null || isJavaFormula || isTransient) {
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

    public ImmutableType getTargetType() {
        return targetType;
    }

    public Set<ImmutableProp> getDependencies() {
        return dependencies;
    }

    public ImmutableProp getIdViewBaseProp() {
        return idViewBaseProp;
    }

    public boolean isVisibilityControllable() {
        return isVisibilityControllable;
    }

    public boolean isRemote() {
        return targetType != null && !declaringType.getMicroServiceName().equals(targetType.getMicroServiceName());
    }

    boolean resolve(TypeUtils typeUtils, int step) {
        switch (step) {
            case 0:
                resolveTargetType(typeUtils);
                return true;
            case 1:
                resolveFormulaDependencies();
                return true;
            case 2:
                resolveIdViewBaseProp();
                return true;
            default:
                return false;
        }
    }

    private void resolveTargetType(TypeUtils typeUtils) {
        if (isAssociation) {
            targetType = typeUtils.getImmutableType(elementType);
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
    }

    private void resolveFormulaDependencies() {
        Formula formula = getAnnotation(Formula.class);
        if (formula == null || formula.dependencies().length == 0) {
            this.dependencies = Collections.emptySet();
        } else {
            Map<String, ImmutableProp> propMap = declaringType.getProps();
            Set<ImmutableProp> props = new LinkedHashSet<>();
            for (String dependency : formula.dependencies()) {
                ImmutableProp prop = propMap.get(dependency);
                if (prop == null) {
                    throw new MetaException(
                            executableElement,
                            "it is decorated by \"@" +
                                    Formula.class.getName() +
                                    "\" but the dependency property \"" +
                                    dependency +
                                    "\" does not eixst"
                    );
                }
                props.add(prop);
                prop.isVisibilityControllable = true;
            }
            this.isVisibilityControllable = true;
            this.dependencies = Collections.unmodifiableSet(props);
        }
    }

    private void resolveIdViewBaseProp() {
        IdView idView = getAnnotation(IdView.class);
        if (idView == null) {
            return;
        }
        String base = idView.value();
        if (base.isEmpty()) {
            if (!isList && name.length() > 2 && !Character.isUpperCase(name.charAt(name.length() - 3)) && name.endsWith("Id")) {
                base = name.substring(0, name.length() - 2);
            } else {
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
        if (!elementTypeName.box().equals(baseProp.targetType.getIdProp().getElementTypeName().box())) {
            throw new MetaException(
                    executableElement,
                    "it is decorated by \"@" +
                            IdView.class.getName() +
                            "\", the base property \"" +
                            baseProp +
                            "\" returns entity type whose id is \"" +
                            baseProp.targetType.getIdProp().getElementTypeName() +
                            "\", but the current property does not return that type"
            );
        }
        baseProp.isVisibilityControllable = true;
        isVisibilityControllable = true;
        idViewBaseProp = baseProp;
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
    @Override
    public String toString() {
        return declaringType.getTypeElement().getQualifiedName().toString() + '.' + name;
    }
}
