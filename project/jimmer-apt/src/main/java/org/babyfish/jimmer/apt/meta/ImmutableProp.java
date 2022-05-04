package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.TypeUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

public class ImmutableProp {

    private TypeElement declaringElement;

    private ExecutableElement executableElement;

    private String name;

    private String getterName;

    private String setterName;

    private String adderByName;

    private String loadedStateName;

    private TypeMirror returnType;

    private TypeName typeName;

    private TypeName draftTypeName;

    private TypeName elementTypeName;

    private TypeName draftElementTypeName;

    private TypeMirror elementType;

    private boolean isList;

    private boolean isAssociation;

    private boolean isNullable;

    public ImmutableProp(
            TypeUtils typeUtils,
            ExecutableElement executableElement
    ) {
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
        } else if (getterName.startsWith("get") &&
                getterName.length() > 3 &&
                Character.isUpperCase(getterName.charAt(3))) {
            name =
                    getterName.substring(3, 4).toLowerCase() +
                            getterName.substring(4);
            setterName = "set" + getterName.substring(3);
            adderByName = "addInto" + getterName.substring(3);
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
            isList = true;
            List<? extends TypeMirror> typeArguments = ((DeclaredType)returnType).getTypeArguments();
            if (typeArguments.isEmpty()) {
                throw new MetaException(
                        String.format(
                                "The return type of '%s' misses generic type",
                                executableElement
                        )
                );
            }
            elementType = typeArguments.get(0);
        } else {
            elementType = returnType;
        }

        isAssociation = typeUtils.isImmutable(elementType);

        if (typeUtils.isEntity(declaringElement) &&
                (isAssociation || isList) &&
                !typeUtils.isEntity(elementType)
        ) {
            throw new MetaException(
                    "Illegal property \"" +
                            this +
                            "\", association property of entity interface must reference to entity type"
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

        draftElementTypeName = elementTypeName;
        if (isAssociation) {
            draftElementTypeName = ClassName.get(
                    ((ClassName)draftElementTypeName).packageName(),
                    ((ClassName)draftElementTypeName).simpleName() + "Draft"
            );
        }
        if (isList) {
            draftTypeName = ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    draftElementTypeName
            );
        } else {
            draftTypeName = draftElementTypeName;
        }

        this.isNullable = determineNullable();
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

    public boolean isList() {
        return isList;
    }

    public boolean isAssociation() {
        return isAssociation;
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

    @Override
    public String toString() {
        return declaringElement.getQualifiedName().toString() + '.' + name;
    }

    private boolean determineNullable() {
        Annotation notNullAnnotation = Arrays.stream(getAnnotations(NotNull.class))
                .findFirst()
                .orElse(null);
        if (notNullAnnotation == null) {
            notNullAnnotation = getAnnotation(NonNull.class);
        }
        if (notNullAnnotation == null) {
            notNullAnnotation = getAnnotation(org.jetbrains.annotations.NotNull.class);
        }

        Annotation nullAnnotation = Arrays.stream(getAnnotations(Null.class))
                .findFirst()
                .orElse(null);
        if (nullAnnotation == null) {
            nullAnnotation = getAnnotation(Nullable.class);
        }
        if (nullAnnotation == null) {
            nullAnnotation = getAnnotation(org.jetbrains.annotations.Nullable.class);
        }

        if (notNullAnnotation != null && nullAnnotation != null) {
            throw new MetaException(
                    "Illegal property \"" +
                            this +
                            "\", it is marked by both @" +
                            notNullAnnotation.annotationType().getName() +
                            " and @" +
                            nullAnnotation.annotationType().getName()
            );
        }

        Boolean implicitNullable = getImplicitNullable();

        if (notNullAnnotation != null) {
            if (Boolean.TRUE.equals(implicitNullable)) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it is marked by @" +
                                notNullAnnotation.annotationType().getName() +
                                ", but its type is consider as nullable type"
                );
            }
            return false;
        }
        if (nullAnnotation != null) {
            if (Boolean.FALSE.equals(implicitNullable)) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it is marked by @" +
                                nullAnnotation.annotationType().getName() +
                                ", but its type is consider as non-null type"
                );
            }
            return true;
        }
        if (implicitNullable != null) {
            return implicitNullable;
        }

        Immutable immutable = declaringElement.getAnnotation(Immutable.class);
        return immutable != null && (immutable.value() == Immutable.Nullity.NULLABLE);
    }

    private Boolean getImplicitNullable() {
        if (isList) {
            return false;
        }
        if (typeName.isPrimitive()) {
            return false;
        }
        if (typeName.isBoxedPrimitive()) {
            return true;
        }
        return null;
    }
}
