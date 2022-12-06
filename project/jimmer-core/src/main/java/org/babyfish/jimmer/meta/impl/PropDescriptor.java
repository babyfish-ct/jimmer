package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.sql.*;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PropDescriptor {

    public static final Set<String> MAPPED_BY_PROVIDER_NAMES = setOf(
            OneToOne.class.getName(), OneToMany.class.getName(), ManyToMany.class.getName()
    );

    private final Type type;

    private final Set<Class<? extends Annotation>> annotationTypes;

    private final boolean isNullable;

    PropDescriptor(Type type, Set<Class<? extends Annotation>> annotationTypes, boolean isNullable) {
        this.type = type;
        this.annotationTypes = annotationTypes;
        this.isNullable = isNullable;
    }

    public Type getType() {
        return type;
    }

    public boolean isPresent(Class<? extends Annotation> annotationType) {
        return annotationTypes.contains(annotationType);
    }

    public Collection<Class<? extends Annotation>> getAnnotationTypes() {
        // For performance, "Collections.unmodifiedCollection" is not called
        return annotationTypes;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public static Builder newBuilder(
            String typeText,
            Class<? extends Annotation> typeAnnotationType,
            String propText,
            String elementText,
            Class<? extends Annotation> elementAnnotationType,
            boolean isList,
            Boolean kotlinNullable,
            Immutable immutable,
            Function<String, RuntimeException> exceptionCreator
    ) {
        return new Builder(
                typeText,
                typeAnnotationType,
                propText,
                elementText,
                elementAnnotationType,
                isList,
                kotlinNullable,
                immutable != null && immutable.value() == Immutable.Nullity.NULLABLE,
                exceptionCreator
        );
    }

    public enum Type {

        TRANSIENT(Transient.class, false),
        ID(Id.class, false),
        VERSION(Version.class, false),
        BASIC(null, false),
        ONE_TO_ONE(OneToOne.class, true),
        MANY_TO_ONE(ManyToOne.class, true),
        ONE_TO_MANY(OneToMany.class, true),
        MANY_TO_MANY(ManyToMany.class, true);

        private final Class<? extends Annotation> annotationType;

        private final boolean isAssociation;

        Type(Class<? extends Annotation> annotationType, boolean isAssociation) {
            this.annotationType = annotationType;
            this.isAssociation = isAssociation;
        }

        public Class<? extends Annotation> getAnnotationType() {
            return annotationType;
        }

        public boolean isAssociation() {
            return isAssociation;
        }

        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '-');
        }
    }

    public static class Builder {

        private static final Map<Class<? extends Annotation>, Type> TYPE_MAP;

        private static final Map<Type, Set<Class<? extends Annotation>>> FAMILY_MAP;

        private static final Map<Class<? extends Annotation>, Set<Type>> INVERSE_MAP;

        private static final Set<Class<? extends Annotation>> SQL_ANNOTATION_TYPES;

        private static final Map<String, Class<? extends Annotation>> ANNOTATION_MAP;

        private static final String VALIDATION_NULL = "javax.validation.constraints.Null";

        private static final String VALIDATION_NOT_NULL = "javax.validation.constraints.NotNull";

        private static final String JETBRAINS_NULLABLE = "org.jetbrains.annotations.Nullable";

        private static final String JETBRAINS_NOT_NULL = "org.jetbrains.annotations.NotNull";

        private static final String SPRINGFRAMEWORK_NULLABLE = "org.springframework.lang.Nullable";

        private static final String SPRINGFRAMEWORK_NON_NULL = "org.springframework.lang.NonNull";

        private static final Set<Class<? extends Annotation>> VALUE_ANNOTATION_TYPES =
                setOf(Entity.class, MappedSuperclass.class, Embeddable.class);

        private static final Set<Class<? extends Annotation>> REF_ANNOTATION_TYPES =
                setOf(Entity.class, MappedSuperclass.class);

        private static final Set<Class<? extends Annotation>> ASSOCIATION_STORAGE_ANNOTATION_TYPES =
                setOf(JoinColumns.class, JoinColumn.class, JoinTable.class);

        private final String typeText;

        private final Class<? extends Annotation> typeAnnotationType;

        private final String propText;

        private final String elementText;

        private final Class<? extends Annotation> elementAnnotationType;

        private final boolean isList;

        private final Boolean kotlinNullable;

        private final boolean defaultNullable;

        private final Function<String, RuntimeException> exceptionCreator;

        private Set<Class<? extends Annotation>> annotationTypes;

        private Class<? extends Annotation> explicitType;

        private Map<Type, Set<Class<? extends Annotation>>> implicitMap;

        private AnnotationNullity annotationNullity;

        private boolean hasMappedBy;

        Builder(
                String typeText,
                Class<? extends Annotation> typeAnnotationType,
                String propText,
                String elementText,
                Class<? extends Annotation> elementAnnotationType,
                boolean isList,
                Boolean kotlinNullable,
                boolean defaultNullable,
                Function<String, RuntimeException> exceptionCreator
        ) {
            this.typeText = typeText;
            this.typeAnnotationType = typeAnnotationType;
            this.propText = propText;
            this.elementText = elementText;
            this.elementAnnotationType = elementAnnotationType;
            this.isList = isList;
            this.kotlinNullable = kotlinNullable;
            this.defaultNullable = defaultNullable;
            this.exceptionCreator = exceptionCreator;
        }

        public Builder add(String annotationTypeName) {
            Class<? extends Annotation> annotationType = ANNOTATION_MAP.get(annotationTypeName);
            if (annotationType != null) {
                add(annotationType);
            }
            addAsNullityAnnotation(annotationTypeName);
            return this;
        }

        public Builder add(Class<? extends Annotation> annotationType) {
            addAsSqlAnnotation(annotationType);
            addAsNullityAnnotation(annotationType.getName());
            return this;
        }

        private void addAsSqlAnnotation(Class<? extends Annotation> annotationType) {
            if (SQL_ANNOTATION_TYPES.contains(annotationType)) {
                Set<Class<? extends Annotation>> declaringTypes =
                        annotationType == Column.class ||
                                annotationType == PropOverrides.class ||
                                annotationType == PropOverride.class ?
                                VALUE_ANNOTATION_TYPES :
                                REF_ANNOTATION_TYPES;
                if (!declaringTypes.contains(typeAnnotationType)) {
                    throw exceptionCreator.apply(
                            "Illegal property \"" +
                                    propText +
                                    "\", it cannot be decorated by @" +
                                    annotationType.getName() +
                                    " because the declaring type \"" +
                                    typeText +
                                    "\" is not decorated by " +
                                    declaringTypes.stream().map(Class::getName).collect(Collectors.toList())
                    );
                }
                Set<Class<? extends Annotation>> ats = annotationTypes;
                if (ats == null) {
                    annotationTypes = ats = new LinkedHashSet<>();
                }
                if (ats.add(annotationType)) {
                    Type type = TYPE_MAP.get(annotationType);
                    if (FAMILY_MAP.containsKey(type)) {
                        if (explicitType != null) {
                            conflict(explicitType, annotationType);
                        }
                        explicitType = annotationType;
                    } else if (explicitType == null) {
                        Set<Type> set = INVERSE_MAP.get(annotationType);
                        if (set == null) {
                            throw new AssertionError(
                                    "Internal bug: Can not determine primary annotation type by @" +
                                            annotationType.getName()
                            );
                        }
                        Map<Type, Set<Class<? extends Annotation>>> newImplicitMap = new LinkedHashMap<>();
                        for (Type implicitType : set) {
                            newImplicitMap
                                    .computeIfAbsent(implicitType, it -> new LinkedHashSet<>())
                                    .add(annotationType);
                        }
                        if (implicitMap == null) {
                            implicitMap = newImplicitMap;
                        } else if (Collections.disjoint(implicitMap.keySet(), newImplicitMap.keySet())) {
                            conflict(implicitMap.values().iterator().next().iterator().next(), annotationType);
                        } else {
                            Iterator<Map.Entry<Type, Set<Class<? extends Annotation>>>> itr =
                                    implicitMap.entrySet().iterator();
                            while (itr.hasNext()) {
                                Map.Entry<Type, Set<Class<? extends Annotation>>> e = itr.next();
                                Set<Class<? extends Annotation>> newSet = newImplicitMap.get(e.getKey());
                                if (newSet == null) {
                                    itr.remove();
                                } else {
                                    e.getValue().addAll(newSet);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void addAsNullityAnnotation(String annotationTypeName) {
            switch (annotationTypeName) {
                case VALIDATION_NULL:
                case JETBRAINS_NULLABLE:
                case SPRINGFRAMEWORK_NULLABLE:
                    addNullityAnnotation(annotationTypeName, true);
                    break;
                case VALIDATION_NOT_NULL:
                case JETBRAINS_NOT_NULL:
                case SPRINGFRAMEWORK_NON_NULL:
                    addNullityAnnotation(annotationTypeName, false);
            }
        }

        public Builder hasMappedBy() {
            hasMappedBy = true;
            return this;
        }

        public PropDescriptor build() {

            if (annotationTypes == null) {
                return new PropDescriptor(Type.BASIC, Collections.emptySet(), determineNullable(Type.BASIC));
            }

            if (annotationTypes.contains(JoinColumns.class) && annotationTypes.contains(JoinTable.class)) {
                conflict(JoinColumns.class, JoinTable.class);
            }
            if (annotationTypes.contains(JoinColumn.class) && annotationTypes.contains(JoinTable.class)) {
                conflict(JoinColumn.class, JoinTable.class);
            }
            if (annotationTypes.contains(Key.class) && annotationTypes.contains(JoinTable.class)) {
                conflict(Key.class, JoinTable.class);
            }
            if (annotationTypes.contains(PropOverrides.class) && annotationTypes.contains(Column.class)) {
                conflict(PropOverrides.class, Column.class);
            }
            if (annotationTypes.contains(PropOverride.class) && annotationTypes.contains(Column.class)) {
                conflict(PropOverride.class, Column.class);
            }
            if (elementAnnotationType == Embeddable.class && annotationTypes.contains(Column.class)) {
                throw exceptionCreator.apply(
                        "Illegal property \"" +
                                propText +
                                "\", embedded property cannot be decorated by @" +
                                Column.class.getName()
                );
            }
            if (elementAnnotationType != Embeddable.class && annotationTypes.contains(PropOverride.class)) {
                throw exceptionCreator.apply(
                        "Illegal property \"" +
                                propText +
                                "\", only embedded property cannot be decorated by @" +
                                PropOverride.class.getName()
                );
            }
            if (elementAnnotationType != Embeddable.class && annotationTypes.contains(PropOverrides.class)) {
                throw exceptionCreator.apply(
                        "Illegal property \"" +
                                propText +
                                "\", only embedded property cannot be decorated by @" +
                                PropOverrides.class.getName()
                );
            }

            Type type;
            Map<Type, Set<Class<? extends Annotation>>> implicitMap = this.implicitMap;
            if (explicitType != null) {
                type = TYPE_MAP.get(explicitType);
            } else if (implicitMap.size() == 1) {
                type = implicitMap.keySet().iterator().next();
            } else if (implicitMap.containsKey(Type.BASIC)) {
                type = Type.BASIC;
            } else {
                throw exceptionCreator.apply(
                        "Illegal property \"" +
                                propText +
                                "\", there are not enough annotations to determine that " +
                                "the current property belongs to one of the following types: " +
                                implicitMap.keySet()
                );
            }
            Set<Class<? extends Annotation>> expectedAnnotationTypes = FAMILY_MAP.get(type);
            for (Class<? extends Annotation> annotationType : annotationTypes) {
                if (annotationType != type.getAnnotationType() &&
                !expectedAnnotationTypes.contains(annotationType)) {
                    throw exceptionCreator.apply(
                            "Illegal property \"" +
                                    propText +
                                    "\", the "+
                                    type +
                                    " property cannot be decorated by @" +
                                    annotationType.getName()
                    );
                }
            }
            validateList(type);
            validateReturnType(type);
            boolean isNullable = determineNullable(type);
            if (hasMappedBy) {
                for (Class<?> annotationType : annotationTypes) {
                    if (ASSOCIATION_STORAGE_ANNOTATION_TYPES.contains(annotationType)) {
                        throw exceptionCreator.apply(
                                "Illegal property \"" +
                                        propText +
                                        "\", it cannot be decorated by @" +
                                        annotationType.getName() +
                                        " because another annotation @" +
                                        type.getAnnotationType().getName() +
                                        " has the argument `mappedBy`"
                        );
                    }
                }
                if (type == Type.ONE_TO_ONE && !isNullable) {
                    throw exceptionCreator.apply(
                            "Illegal property \"" +
                                    propText +
                                    "\", its annotation @" +
                                    type.getAnnotationType().getName() +
                                    " has the argument `mappedBy` so that it must be nullable"
                    );
                }
            }
            return new PropDescriptor(type, annotationTypes, isNullable);
        }

        private void validateList(Type type) {
            switch (type) {
                case TRANSIENT:
                    break;
                case ONE_TO_MANY:
                case MANY_TO_MANY:
                    if (!isList) {
                        throw exceptionCreator.apply(
                                "The property \"" +
                                        propText +
                                        "\" is illegal, it is not list so that it cannot be decorated by @" +
                                        type.getAnnotationType().getName()
                        );
                    }
                    break;
                default:
                    if (isList) {
                        throw exceptionCreator.apply(
                                "The property \"" +
                                        propText +
                                        "\" is illegal, list property must be decorated by @" +
                                        OneToMany.class +
                                        "or @" +
                                        ManyToMany.class
                        );
                    }
                    break;
            }
        }

        private void validateReturnType(Type type) {
            if (type.isAssociation() && elementAnnotationType != Entity.class) {
                throw exceptionCreator.apply(
                        "The property \"" +
                                propText +
                                "\" is illegal, it is association property so that its target type \"" +
                                elementText +
                                "\" must be decorated by @" +
                                Entity.class.getName()
                );
            }
            if (type != Type.TRANSIENT &&
                    !type.isAssociation() &&
                    elementAnnotationType != null &&
                    elementAnnotationType != Embeddable.class) {
                throw exceptionCreator.apply(
                        "The property \"" +
                                propText +
                                "\" is illegal, it is not association property, its target type \"" +
                                elementText +
                                "\" is immutable type, immutable type is not enough, please use @" +
                                Entity.class.getName()
                );
            }
        }

        private void addNullityAnnotation(String annotationTypeName, boolean nullable) {
            if (kotlinNullable != null) {
                throw exceptionCreator.apply(
                        "The property \"" +
                                propText +
                                "\" is illegal, it cannot be decorated by @" +
                                annotationTypeName +
                                " because its nullity hash already specified by kotlin language"
                );
            }
            if (annotationNullity != null) {
                if (annotationNullity.isNullable != nullable) {
                    throw exceptionCreator.apply(
                            "The property \"" +
                                    propText +
                                    "\" is illegal, it cannot be decorated by both @" +
                                    annotationNullity.annotationTypeName +
                                    " and @" +
                                    annotationTypeName
                    );
                }
            } else {
                annotationNullity = new AnnotationNullity(annotationTypeName, nullable);
            }
        }

        private boolean determineNullable(Type type) {
            boolean specifiedNullable = kotlinNullable != null ?
                    kotlinNullable :
                    annotationNullity != null ?
                            annotationNullity.isNullable :
                            defaultNullable;
            switch (type) {
                case ID:
                case VERSION:
                case ONE_TO_MANY:
                case MANY_TO_MANY:
                    if (specifiedNullable) {
                        throw exceptionCreator.apply(
                                "Illegal property \"" +
                                        propText +
                                        "\", it cannot be nullable because it is " +
                                        type +
                                        " property"
                        );
                    }
                    break;
                case ONE_TO_ONE:
                case MANY_TO_ONE:
                    if (annotationTypes.contains(JoinTable.class) && !specifiedNullable) {
                        throw exceptionCreator.apply(
                                "Illegal property \"" +
                                        propText +
                                        "\", the " +
                                        type +
                                        " property decorated by @" +
                                        JoinTable.class +
                                        " must be nullable"
                        );
                    }
                    break;
            }
            return specifiedNullable;
        }

        private void conflict(Class<?> annotationType1, Class<?> annotationType2) {
            throw exceptionCreator.apply(
                    "Illegal property \"" +
                            propText +
                            "\", it cannot be decorated by both @" +
                            annotationType1.getName() +
                            " and @" +
                            annotationType2.getName()
            );
        }

        static {

            Map<Class<? extends Annotation>, Type> typeMap = new LinkedHashMap<>();
            Map<Type, Set<Class<? extends Annotation>>> families = new LinkedHashMap<>();
            Map<Class<? extends Annotation>, Set<Type>> inverseMap = new LinkedHashMap<>();
            Set<Class<? extends Annotation>> sqlTypes = new LinkedHashSet<>();
            Map<String, Class<? extends Annotation>> annotationMap = new LinkedHashMap<>();

            typeMap.put(Transient.class, Type.TRANSIENT);
            typeMap.put(Id.class, Type.ID);
            typeMap.put(Version.class, Type.VERSION);
            typeMap.put(OneToOne.class, Type.ONE_TO_ONE);
            typeMap.put(ManyToOne.class, Type.MANY_TO_ONE);
            typeMap.put(OneToMany.class, Type.ONE_TO_MANY);
            typeMap.put(ManyToMany.class, Type.MANY_TO_MANY);

            families.put(Type.TRANSIENT, setOf());
            families.put(Type.ID, setOf(Column.class, PropOverrides.class, PropOverride.class));
            families.put(Type.VERSION, setOf(Column.class));
            families.put(Type.BASIC, setOf(Key.class, Column.class, PropOverrides.class, PropOverride.class));
            families.put(Type.ONE_TO_ONE, setOf(Key.class, OnDissociate.class, JoinColumns.class, JoinColumn.class, JoinTable.class));
            families.put(Type.MANY_TO_ONE, setOf(Key.class, OnDissociate.class, JoinColumns.class, JoinColumn.class, JoinTable.class));
            families.put(Type.ONE_TO_MANY, setOf());
            families.put(Type.MANY_TO_MANY, setOf(JoinTable.class));

            for (Map.Entry<Type, Set<Class<? extends Annotation>>> e : families.entrySet()) {
                Type type = e.getKey();
                if (type.getAnnotationType() != null) {
                    sqlTypes.add(type.getAnnotationType());
                }
                for (Class<? extends Annotation> annotationType : e.getValue()) {
                    sqlTypes.add(annotationType);
                    inverseMap
                            .computeIfAbsent(annotationType, it -> new LinkedHashSet<>())
                            .add(type);
                }
            }

            for (Class<? extends Annotation> annotationType : sqlTypes) {
                annotationMap.put(annotationType.getName(), annotationType);
            }
            annotationMap.put(javax.validation.constraints.Null.class.getName(), javax.validation.constraints.Null.class);
            annotationMap.put(org.jetbrains.annotations.Nullable.class.getName(), org.jetbrains.annotations.Nullable.class);
            annotationMap.put(javax.validation.constraints.NotNull.class.getName(), javax.validation.constraints.NotNull.class);
            annotationMap.put(org.jetbrains.annotations.NotNull.class.getName(), org.jetbrains.annotations.NotNull.class);

            TYPE_MAP = typeMap;
            FAMILY_MAP = families;
            INVERSE_MAP = inverseMap;
            SQL_ANNOTATION_TYPES = sqlTypes;
            ANNOTATION_MAP = annotationMap;
        }
    }

    private static class AnnotationNullity {

        final String annotationTypeName;

        final boolean isNullable;

        private AnnotationNullity(String annotationTypeName, boolean isNullable) {
            this.annotationTypeName = annotationTypeName;
            this.isNullable = isNullable;
        }
    }

    @SafeVarargs
    private static <E> Set<E> setOf(E ... elements) {
        switch (elements.length) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(elements[0]);
            default:
                return new LinkedHashSet<>(Arrays.asList(elements));
        }
    }
}
