package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.sql.Key;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.lang.annotation.Annotation;
import java.util.*;

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

    private Annotation associationAnnotation;

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

        if (declaringElement.getAnnotation(Entity.class) != null &&
                (isAssociation || isList) &&
                !typeUtils.isEntity(elementType)
        ) {
            throw new MetaException(
                    "Illegal property \"" +
                            this +
                            "\", association property of entity interface must reference to entity type"
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

    public Annotation getAssociationAnnotation() {
        return associationAnnotation;
    }

    @Override
    public String toString() {
        return declaringElement.getQualifiedName().toString() + '.' + name;
    }

    private void initializeAssociationAnnotation() {
        Transient trans = getAnnotation(Transient.class);

        JoinColumn[] joinColumns = getAnnotations(JoinColumn.class);
        JoinTable joinTable = getAnnotation(JoinTable.class);
        Column column = getAnnotation(Column.class);
        Annotation[] storageAnnotations = Arrays.stream(
                new Annotation[] {
                        joinColumns.length == 0 ? null : joinColumns[0],
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
                                "\", it is marked by both @" +
                                Transient.class.getName() +
                                " and @" +
                                firstSqlAnnotation.annotationType().getName()
                );
            }
            return;
        }

        if (declaringElement.getAnnotation(Entity.class) == null) {
            if (firstSqlAnnotation != null) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it cannot be marked by @" +
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
                                    "\", association property must be marked by one of these annotations: " +
                                    "@OneToOne, @OneToMany, @ManyToOne or @ManyToMany"
                    );
                }
                if (associationAnnotations.length > 1) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it cannot be marked by both @" +
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
                                    "\", list property cannot be marked by both @" +
                                    associationAnnotation.annotationType().getName()
                    );
                }
                if (!isList && (associationAnnotation instanceof OneToMany ||
                        associationAnnotation instanceof ManyToMany)) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", reference property cannot be marked by both @" +
                                    associationAnnotation.annotationType().getName()
                    );
                }
                if (oneToOne != null && oneToOne.mappedBy().equals("")) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", one-to-one property must be mapped by another reference property"
                    );
                }
                if (oneToMany != null && oneToMany.mappedBy().equals("")) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", one-to-many property must be mapped by another reference property"
                    );
                }
                if (oneToOne != null || oneToMany != null || (
                        manyToMany != null && !manyToMany.mappedBy().equals(""))
                ) {
                    if (joinColumns.length != 0) {
                        throw new MetaException(
                                "Illegal property \"" +
                                        this +
                                        "\", mapped property cannot be marked by @" +
                                        associationAnnotation.annotationType().getName()
                        );
                    }
                }
                if (column != null) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", association property cannot be marked by @" +
                                    column.annotationType().getName()
                    );
                }
                if (joinColumns.length != 0 && joinTable != null) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it is marked by both @JoinColumn and @JoinTable"
                    );
                }
                if (joinColumns.length > 1) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", multiple join columns is not supported"
                    );
                }
                if (Arrays
                        .stream(joinColumns)
                        .anyMatch(it -> !it.referencedColumnName().isEmpty())) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", the referenced column of join column is not supported"
                    );
                }
                if (scalarAnnotations.length != 0) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", association property cannot be marked by @" +
                                    scalarAnnotations[0].annotationType().getName()
                    );
                }
                if (joinTable != null && joinTable.joinColumns().length > 1) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", join table with multiple join columns is not supported"
                    );
                }
                if (joinTable != null &&
                        Arrays.stream(joinTable.joinColumns())
                                .anyMatch(it -> !it.referencedColumnName().isEmpty())) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", the referenced column of join column is not supported"
                    );
                }
                if (joinTable != null && joinTable.inverseJoinColumns().length > 1) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", join table with multiple inverse join columns is not supported"
                    );
                }
                if (joinTable != null &&
                        Arrays.stream(joinTable.inverseJoinColumns())
                                .anyMatch(it -> !it.referencedColumnName().isEmpty())) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", the referenced column of inverse join column is not supported"
                    );
                }
            } else {
                if (associationAnnotations.length != 0) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", scalar property cannot be marked by @" +
                                    associationAnnotations[0].annotationType().getName()
                    );
                }
                if (joinColumns.length != 0) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", scalar property cannot be marked by @" +
                                    JoinColumn.class.getName()
                    );
                }
                if (joinTable != null) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", scalar property cannot be marked by @" +
                                    JoinTable.class.getName()
                    );
                }
                if (scalarAnnotations.length > 1) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it is marked by both @" +
                                    storageAnnotations[0].annotationType().getName() +
                                    " @" +
                                    storageAnnotations[1].annotationType().getName()
                    );
                }
                if (scalarAnnotations.length != 0 && isNullable) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", nullable property cannot be marked by @" +
                                    scalarAnnotations[0].annotationType().getName()
                    );
                }
                if (version != null && returnType.getKind() != TypeKind.INT) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    this +
                                    "\", it is marked by @" +
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
                                    "\", it marked by both \"@" +
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
                                        "\", association property marked by both \"@" +
                                        Key.class.getName() +
                                        "\" must be many-to-one association"
                        );
                    }
                    if (joinTable != null) {
                        throw new MetaException(
                                "Illegal property \"" +
                                        this +
                                        "\", many-to-one property marked by both \"@" +
                                        Key.class.getName() +
                                        "\" must base on foreign key"
                        );
                    }
                }
            }
        }
    }

    private boolean determineNullable() {

        AnnotationRef notNullAnnotationRef = Arrays.stream(getAnnotations(NotNull.class))
                .findFirst()
                .map(it -> AnnotationRef.of(it))
                .orElse(null);
        if (notNullAnnotationRef == null) {
            notNullAnnotationRef = AnnotationRef.of(getAnnotation(NonNull.class));
        }
        if (notNullAnnotationRef == null) {
            notNullAnnotationRef = AnnotationRef.of(getAnnotation(org.jetbrains.annotations.NotNull.class));
        }
        if (notNullAnnotationRef == null) {
            if (getAnnotation(Id.class) != null) {
                notNullAnnotationRef = new AnnotationRef(Id.class);
            }
        }
        if (notNullAnnotationRef == null) {
            ManyToOne manyToOne = getAnnotation(ManyToOne.class);
            if (manyToOne != null && !manyToOne.optional()) {
                notNullAnnotationRef = new AnnotationRef(ManyToOne.class, "optional", false);
            }
        }
        if (notNullAnnotationRef == null) {
            OneToOne oneToOne = getAnnotation(OneToOne.class);
            if (oneToOne != null && !oneToOne.optional()) {
                notNullAnnotationRef = new AnnotationRef(OneToOne.class, "optional", false);
            }
        }
        if (notNullAnnotationRef == null) {
            notNullAnnotationRef = Arrays.stream(getAnnotations(Column.class))
                    .filter(it -> !it.nullable())
                    .findFirst()
                    .map(it -> new AnnotationRef(Column.class, "nullable", false))
                    .orElse(null);
        }
        if (notNullAnnotationRef == null) {
            notNullAnnotationRef = Arrays.stream(getAnnotations(JoinColumn.class))
                    .filter(it -> !it.nullable())
                    .findFirst()
                    .map(it -> new AnnotationRef(JoinColumn.class, "nullable", false))
                    .orElse(null);
        }

        AnnotationRef nullAnnotationRef = Arrays.stream(getAnnotations(Null.class))
                .findFirst()
                .map(it -> AnnotationRef.of(it))
                .orElse(null);
        if (nullAnnotationRef == null) {
            nullAnnotationRef = AnnotationRef.of(getAnnotation(Nullable.class));
        }
        if (nullAnnotationRef == null) {
            nullAnnotationRef = AnnotationRef.of(getAnnotation(org.jetbrains.annotations.Nullable.class));
        }
        if (nullAnnotationRef == null) {
            ManyToOne manyToOne = getAnnotation(ManyToOne.class);
            if (manyToOne != null && manyToOne.optional()) {
                nullAnnotationRef = new AnnotationRef(ManyToOne.class, "optional", true);
            }
        }
        if (nullAnnotationRef == null) {
            OneToOne oneToOne = getAnnotation(OneToOne.class);
            if (oneToOne != null && oneToOne.optional()) {
                nullAnnotationRef = new AnnotationRef(OneToOne.class, "optional", true);
            }
        }
        if (nullAnnotationRef == null) {
            nullAnnotationRef = Arrays.stream(getAnnotations(Column.class))
                    .filter(Column::nullable)
                    .findFirst()
                    .map(it -> new AnnotationRef(Column.class, "nullable", true))
                    .orElse(null);
        }
        if (nullAnnotationRef == null) {
            nullAnnotationRef = Arrays.stream(getAnnotations(JoinColumn.class))
                    .filter(JoinColumn::nullable)
                    .findFirst()
                    .map(it -> new AnnotationRef(JoinColumn.class, "nullable", true))
                    .orElse(null);
        }

        if (notNullAnnotationRef != null && nullAnnotationRef != null) {
            throw new MetaException(
                    "Illegal property \"" +
                            this +
                            "\", its nullity is conflict because it is marked by both " +
                            notNullAnnotationRef +
                            " and " +
                            nullAnnotationRef
            );
        }

        ImplicitNullable implicitNullable = getImplicitNullable();

        if (notNullAnnotationRef != null) {
            if (implicitNullable != null && implicitNullable.nullable) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it is marked by " +
                                notNullAnnotationRef +
                                ", but it is considered as nullable because " +
                                implicitNullable.reason
                );
            }
            return false;
        }
        if (nullAnnotationRef != null) {
            if (implicitNullable != null && !implicitNullable.nullable) {
                throw new MetaException(
                        "Illegal property \"" +
                                this +
                                "\", it is marked by " +
                                nullAnnotationRef +
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

    private static class AnnotationRef {

        private Class<? extends Annotation> annotationType;

        private Map<String, Object> valueMap;

        public static AnnotationRef of(Annotation annotation) {
            return annotation == null ?
                    null :
                    new AnnotationRef(annotation.annotationType());
        }

        public AnnotationRef(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        public AnnotationRef(
                Class<? extends Annotation> annotationType,
                String attrName,
                Object attrValue
        ) {
            this.annotationType = annotationType;
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put(attrName, attrValue);
            this.valueMap = valueMap;
        }

        public AnnotationRef(
                Class<Annotation> annotationType,
                Map<String, Object> valueMap
        ) {
            this.annotationType = annotationType;
            this.valueMap = valueMap;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('@');
            builder.append(annotationType.getName());
            if (valueMap != null && !valueMap.isEmpty()) {
                builder.append('(');
                boolean addComma = false;
                for (Map.Entry<String, Object> e : valueMap.entrySet()) {
                    if (addComma) {
                        builder.append(", ");
                    } else {
                        addComma = true;
                    }
                    builder
                            .append(e.getKey())
                            .append('=')
                            .append(e.getValue());
                }
                builder.append(')');
            }
            return builder.toString();
        }
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
