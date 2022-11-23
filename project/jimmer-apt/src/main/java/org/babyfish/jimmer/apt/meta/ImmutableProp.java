package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.sql.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
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

        if (declaringElement.getAnnotation(Entity.class) != null &&
                (isAssociation || isList) &&
                !typeUtils.isEntity(elementType) &&
                trans == null
        ) {
            throw new MetaException(
                    "Illegal property \"" +
                            this +
                            "\", association property of entity interface " +
                            "must reference to entity type or decorated by @Transient"
            );
        }

        initializeAssociationAnnotation();

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

        this.isNullable = determineNullable();
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

    public Annotation getAssociationAnnotation() {
        return associationAnnotation;
    }

    @Override
    public String toString() {
        return declaringElement.getQualifiedName().toString() + '.' + name;
    }

    private void initializeAssociationAnnotation() {
        Transient trans = getAnnotation(Transient.class);

        JoinColumn joinColumn = getAnnotation(JoinColumn.class);
        JoinTable joinTable = getAnnotation(JoinTable.class);
        Column column = getAnnotation(Column.class);
        Annotation[] storageAnnotations = Arrays.stream(
                new Annotation[] {
                        joinColumn,
                        joinTable,
                        column
                }
        ).filter(Objects::nonNull).toArray(Annotation[]::new);

        Id id = getAnnotation(Id.class);
        Version version = getAnnotation(Version.class);
        Key key = getAnnotation(Key.class);
        Annotation[] scalarAnnotations = Arrays.stream(
                new Annotation[] { id, version }
        ).filter(Objects::nonNull).toArray(Annotation[]::new);

        OneToOne oneToOne = getAnnotation(OneToOne.class);
        OneToMany oneToMany = getAnnotation(OneToMany.class);
        ManyToOne manyToOne = getAnnotation(ManyToOne.class);
        ManyToMany manyToMany = getAnnotation(ManyToMany.class);
        OnDissociate onDissociate = getAnnotation(OnDissociate.class);
        Annotation[] associationAnnotations = Arrays.stream(
                new Annotation[] { oneToOne, oneToMany, manyToOne, manyToMany }
        ).filter(Objects::nonNull).toArray(Annotation[]::new);

        Annotation firstSqlAnnotation = storageAnnotations.length != 0 ? storageAnnotations[0] : null;
        if (firstSqlAnnotation == null) {
            firstSqlAnnotation = scalarAnnotations.length != 0 ? scalarAnnotations[0] : null;
            if (firstSqlAnnotation == null && associationAnnotations.length != 0) {
                firstSqlAnnotation = associationAnnotations[0];
            }
        }

        if (trans != null) {
            if (firstSqlAnnotation != null) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it is decorated by both @" +
                                Transient.class.getName() +
                                " and @" +
                                firstSqlAnnotation.annotationType().getName()
                );
            }
            return;
        }

        if (declaringElement.getAnnotation(Entity.class) == null &&
                declaringElement.getAnnotation(MappedSuperclass.class) == null) {
            if (firstSqlAnnotation != null) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it cannot be decorated by @" +
                                firstSqlAnnotation.annotationType().getName() +
                                "because the current type is not entity"
                );
            }
        } else {
            if (isAssociation) {
                if (associationAnnotations.length == 0) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", association property must be decorated by one of these annotations: " +
                                    "@OneToOne, @OneToMany, @ManyToOne or @ManyToMany"
                    );
                }
                if (associationAnnotations.length > 1) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it cannot be decorated by both @" +
                                    associationAnnotations[0].annotationType().getName() +
                                    "and @" +
                                    associationAnnotations[1].annotationType().getName()
                    );
                }
                associationAnnotation = associationAnnotations[0];
                if (isList && (associationAnnotation instanceof OneToOne ||
                        associationAnnotation instanceof ManyToOne)) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", list property cannot be decorated by both @" +
                                    associationAnnotation.annotationType().getName()
                    );
                }
                if (!isList && (associationAnnotation instanceof OneToMany ||
                        associationAnnotation instanceof ManyToMany)) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", reference property cannot be decorated by both @" +
                                    associationAnnotation.annotationType().getName()
                    );
                }
                if (oneToMany != null && oneToMany.mappedBy().equals("")) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", The \"mappedBy\" of one-to-many property must be specified"
                    );
                }
                boolean isMappedBy =
                        oneToOne != null && !oneToOne.mappedBy().isEmpty() ||
                                oneToMany != null ||
                                manyToMany != null && !manyToMany.mappedBy().isEmpty();
                if (isMappedBy && storageAnnotations.length != 0) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", the property with \"mappedBy\" cannot be decorated by @" +
                                    storageAnnotations[0].annotationType().getName()
                    );
                }
                if (column != null) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", association property cannot be decorated by @" +
                                    column.annotationType().getName()
                    );
                }
                if (joinColumn != null && joinTable != null) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it is decorated by both @JoinColumn and @JoinTable"
                    );
                }
                if (scalarAnnotations.length != 0) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", association property cannot be decorated by @" +
                                    scalarAnnotations[0].annotationType().getName()
                    );
                }
            } else {
                if (associationAnnotations.length != 0) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", scalar property cannot be decorated by @" +
                                    associationAnnotations[0].annotationType().getName()
                    );
                }
                if (joinColumn != null) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", scalar property cannot be decorated by @" +
                                    JoinColumn.class.getName()
                    );
                }
                if (joinTable != null) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", scalar property cannot be decorated by @" +
                                    JoinTable.class.getName()
                    );
                }
                if (scalarAnnotations.length > 1) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it is decorated by both @" +
                                    storageAnnotations[0].annotationType().getName() +
                                    " @" +
                                    storageAnnotations[1].annotationType().getName()
                    );
                }
                if (scalarAnnotations.length != 0 && isNullable) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", nullable property cannot be decorated by @" +
                                    scalarAnnotations[0].annotationType().getName()
                    );
                }
                if (version != null && returnType.getKind() != TypeKind.INT) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it is decorated by @" +
                                    Version.class.getName() +
                                    " but its type is not int"
                    );
                }
            }
            if (key != null) {
                if (scalarAnnotations.length != 0) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it decorated by both \"@" +
                                    Key.class.getName() +
                                    "\" and \"@" +
                                    scalarAnnotations[0].annotationType().getName() +
                                    "\""
                    );
                }
                if (associationAnnotation != null) {
                    if (associationAnnotation.annotationType() != ManyToOne.class) {
                        throw new MetaException(
                                "Illegal property \"" +
                                        this +
                                        "\", association property decorated by both \"@" +
                                        Key.class.getName() +
                                        "\" must be many-to-one association"
                        );
                    }
                    if (joinTable != null) {
                        throw new MetaException(
                                "Illegal property \"" +
                                        this +
                                        "\", many-to-one property decorated by \"@" +
                                        Key.class.getName() +
                                        "\" must base on foreign key"
                        );
                    }
                }
            }
            if (onDissociate != null) {
                if (oneToOne != null && !oneToOne.mappedBy().isEmpty()) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", the property with \"mappedBy\" cannot be decorated by \"@" +
                                    OnDissociate.class.getName()
                    );
                }
                if (oneToOne == null && manyToOne == null) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", the property decorated by \"@" +
                                    OnDissociate.class.getName() +
                                    "\" must be many-to-one or one-to-one property"
                    );
                }
            }
        }
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
        if (notNullAnnotation == null) {
            notNullAnnotation = getAnnotation(Id.class);
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
                            "\", its nullity is conflict because it is decorated by both " +
                            notNullAnnotation +
                            " and " +
                            nullAnnotation
            );
        }

        ImplicitNullable implicitNullable = getImplicitNullable();

        if (notNullAnnotation != null) {
            if (implicitNullable != null && implicitNullable.nullable) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it is decorated by " +
                                notNullAnnotation +
                                ", but it is considered as nullable because " +
                                implicitNullable.reason
                );
            }
            return false;
        }
        if (nullAnnotation != null) {
            if (implicitNullable != null && !implicitNullable.nullable) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it is decorated by " +
                                nullAnnotation +
                                ", but it is considered as non-null because " +
                                implicitNullable.reason
                );
            }
            return true;
        }
        if (implicitNullable != null) {
            return implicitNullable.nullable;
        }

        Immutable immutable = declaringElement.getAnnotation(Immutable.class);
        return immutable != null && (immutable.value() == Immutable.Nullity.NULLABLE);
    }

    private ImplicitNullable getImplicitNullable() {
        if (isList) {
            return new ImplicitNullable(false, "it is list");
        } else if (isAssociation) {
            if (getAnnotation(JoinTable.class) != null) {
                return new ImplicitNullable(
                        true,
                        "it's a many-to-one association base on middle table"
                );
            }
            if (getAnnotation(OneToOne.class) != null) {
                return new ImplicitNullable(
                        true,
                        "it's a one-to-one association"
                );
            }
        }
        if (typeName.isPrimitive()) {
            return new ImplicitNullable(false, "its type is primitive");
        }
        if (typeName.isBoxedPrimitive()) {
            return new ImplicitNullable(true, "its type is box type");
        }
        return null;
    }

    public Map<ClassName, String> getValidationMessageMap() {
        return validationMessageMap;
    }

    private static class ImplicitNullable {

        final boolean nullable;

        final String reason;

        ImplicitNullable(boolean nullable, String reason) {
            this.nullable = nullable;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "ImplicitNullable{" +
                    "nullable=" + nullable +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
}
