package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
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

    private final TypeElement declaringElement;

    private final ExecutableElement executableElement;

    private final String name;

    private final int id;

    private final String getterName;

    private final String setterName;

    private final String adderByName;

    private final boolean beanStyle;

    private final String loadedStateName;

    private final TypeMirror returnType;

    private final TypeName typeName;

    private final TypeName draftTypeName;

    private final TypeName elementTypeName;

    private final TypeName draftElementTypeName;

    private final TypeMirror elementType;

    private final boolean isTransient;

    private final boolean isList;

    private final boolean isAssociation;

    private final boolean isEntityAssociation;

    private final boolean isNullable;

    private Annotation associationAnnotation;

    private final Map<ClassName, String> validationMessageMap;

    public ImmutableProp(
            TypeUtils typeUtils,
            ExecutableElement executableElement,
            int id
    ) {
        this.id = id;
        this.declaringElement = (TypeElement) executableElement.getEnclosingElement();
        this.executableElement = executableElement;
        getterName = executableElement.getSimpleName().toString();
        returnType = executableElement.getReturnType();
        if (returnType.getKind() == TypeKind.VOID) {
            throw new MetaException(
                    String.format(
                            "'%s' cannot return void",
                            executableElement
                    )
            );
        }
        if (!executableElement.getParameters().isEmpty()) {
            throw new MetaException(
                    String.format(
                            "'%s' cannot have parameters",
                            executableElement
                    )
            );
        }

        if (getterName.startsWith("is") &&
                getterName.length() > 2 &&
                Character.isUpperCase(getterName.charAt(2))) {
            name =
                    getterName.substring(2, 3).toLowerCase() +
                    getterName.substring(3);
            setterName = "set" + getterName.substring(2);
            adderByName = "addInto" + getterName.substring(2);
            beanStyle = true;
        } else if (getterName.startsWith("get") &&
                getterName.length() > 3 &&
                Character.isUpperCase(getterName.charAt(3))) {
            name =
                    getterName.substring(3, 4).toLowerCase() +
                            getterName.substring(4);
            setterName = "set" + getterName.substring(3);
            adderByName = "addInto" + getterName.substring(3);
            beanStyle = true;
        } else {
            name = getterName;
            setterName =
                    "set" +
                    getterName.substring(0, 1).toUpperCase() +
                    getterName.substring(1);
            adderByName =
                    "addInto" +
                    getterName.substring(0, 1).toUpperCase() +
                    getterName.substring(1);
            beanStyle = false;
        }

        loadedStateName = name + "Loaded";

        if (typeUtils.isCollection(returnType)) {
            if (!typeUtils.isListStrictly(returnType)) {
                throw new MetaException(
                        String.format(
                                "The collection property '%s' must return 'java.util.List'",
                                executableElement
                        )
                );
            }
            List<? extends TypeMirror> typeArguments = ((DeclaredType)returnType).getTypeArguments();
            if (typeArguments.isEmpty()) {
                throw new MetaException(
                        String.format(
                                "The return type of '%s' misses generic type",
                                executableElement
                        )
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
                    "Illegal property \"" +
                            this +
                            "\", the target type \"" +
                            TypeName.get(elementType) +
                            "\" is illegal, it cannot be type decorated by @MappedSuperclass"
            );
        }

        Transient trans = executableElement.getAnnotation(Transient.class);
        isTransient = trans != null;
        isAssociation = typeUtils.isImmutable(elementType);
        isEntityAssociation = typeUtils.isEntity(elementType);
        if (isList && typeUtils.isEmbeddable(elementType)) {
            throw new MetaException(
                    "Illegal property \"" +
                            this +
                            "\", the target type \"" +
                            TypeName.get(elementType) +
                            "\" is embeddable so that the property type cannot be list"
            );
        }

        PropDescriptor.Builder builder = PropDescriptor.newBuilder(
                declaringElement.getQualifiedName().toString(),
                typeUtils.getImmutableAnnotationType(declaringElement),
                this.toString(),
                ClassName.get(elementType).toString(),
                typeUtils.getImmutableAnnotationType(elementType),
                isList,
                null,
                declaringElement.getAnnotation(Immutable.class),
                MetaException::new
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

        elementTypeName = TypeName.get(elementType);
        if (isList) {
            typeName = ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    elementTypeName
            );
        } else {
            typeName = elementTypeName;
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

    public String getAdderByName() {
        return adderByName;
    }

    public boolean isBeanStyle() { return beanStyle; }

    public String getLoadedStateName() {
        if (!isLoadedStateRequired()) {
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

    public TypeName getDraftElementTypeName() {
        return draftElementTypeName;
    }

    public boolean isTransient() {
        return isTransient;
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

    public boolean isLoadedStateRequired() {
        return isNullable || typeName.isPrimitive();
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

    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return executableElement.getAnnotation(annotationType);
    }

    public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
        return executableElement.getAnnotationsByType(annotationType);
    }

    public Annotation getAssociationAnnotation() {
        return associationAnnotation;
    }

    @Override
    public String toString() {
        return declaringElement.getQualifiedName().toString() + '.' + name;
    }

    public Map<ClassName, String> getValidationMessageMap() {
        return validationMessageMap;
    }
}
