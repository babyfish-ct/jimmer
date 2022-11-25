package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.sql.*;

import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotationFamily {

    private static final Map<Class<? extends Annotation>, Type> TYPE_MAP;

    private static final Map<Type, Set<Class<? extends Annotation>>> FAMILY_MAP;

    private static final Map<Class<? extends Annotation>, Set<Type>> INVERSE_MAP;

    private static final Set<Class<? extends Annotation>> ALL_ANNOTATION_TYPES;

    private final Type type;

    private final Set<Class<? extends Annotation>> annotationTypes;

    AnnotationFamily(Type type, Set<Class<? extends Annotation>> annotationTypes) {
        this.type = type;
        this.annotationTypes = annotationTypes;
    }

    public Type getType() {
        return type;
    }

    public boolean isPresent(Class<? extends Annotation> annotationType) {
        return annotationTypes.contains(annotationType);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public enum Type {

        TRANSIENT(Transient.class),
        ID(Id.class),
        VERSION(Version.class),
        BASIC(null),
        EMBEDDED(null),
        ONE_TO_ONE(OneToOne.class),
        MANY_TO_ONE(ManyToOne.class),
        ONE_TO_MANY(OneToMany.class),
        MANY_TO_MANY(ManyToMany.class);

        private final Class<? extends Annotation> annotationType;

        Type(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        public Class<? extends Annotation> getAnnotationType() {
            return annotationType;
        }
    }

    public static class Builder {

        private Set<Class<? extends Annotation>> annotationTypes;

        private Class<? extends Annotation> explicitType;

        private Map<Type, Set<Class<? extends Annotation>>> implicitMap;

        public Builder add(Class<? extends Annotation> annotationType) throws ConflictException {
            if (ALL_ANNOTATION_TYPES.contains(annotationType)) {
                Set<Class<? extends Annotation>> ats = annotationTypes;
                if (ats == null) {
                    annotationTypes = ats = new LinkedHashSet<>();
                }
                if (ats.add(annotationType)) {
                    Type type = TYPE_MAP.get(annotationType);
                    if (FAMILY_MAP.containsKey(type)) {
                        if (explicitType != null) {
                            throw new ConflictException(explicitType, annotationType);
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
                            throw new ConflictException(
                                    implicitMap.values().iterator().next().iterator().next(),
                                    annotationType
                            );
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
            return this;
        }

        public AnnotationFamily build() throws ConflictException, IndistinguishableException, UnexpectedException {
            if (annotationTypes == null) {
                return new AnnotationFamily(Type.BASIC, Collections.emptySet());
            }
            if (annotationTypes.contains(JoinColumns.class) && annotationTypes.contains(JoinTable.class)) {
                throw new ConflictException(JoinColumns.class, JoinTable.class);
            }
            if (annotationTypes.contains(JoinColumn.class) && annotationTypes.contains(JoinTable.class)) {
                throw new ConflictException(JoinColumn.class, JoinTable.class);
            }
            Type type;
            Map<Type, Set<Class<? extends Annotation>>> implicitMap = this.implicitMap;
            if (explicitType != null) {
                type = TYPE_MAP.get(explicitType);
            } else if (implicitMap.size() == 1) {
                type = implicitMap.keySet().iterator().next();
            } else if (annotationTypes.size() == 1 && annotationTypes.iterator().next() == Key.class) {
                type = Type.BASIC;
            } else {
                throw new IndistinguishableException(implicitMap.keySet());
            }
            Set<Class<? extends Annotation>> expectedAnnotationTypes = FAMILY_MAP.get(type);
            for (Class<? extends Annotation> annotationType : annotationTypes) {
                if (annotationType != type.getAnnotationType() &&
                !expectedAnnotationTypes.contains(annotationType)) {
                    throw new UnexpectedException(type, annotationType);
                }
            }
            return new AnnotationFamily(type, annotationTypes);
        }
    }

    public static class ConflictException extends Exception {

        private final Class<? extends Annotation> annotationType1;

        private final Class<? extends Annotation> annotationType2;

        ConflictException(
                Class<? extends Annotation> annotationType1,
                Class<? extends Annotation> annotationType2
        ) {
            this.annotationType1 = annotationType1;
            this.annotationType2 = annotationType2;
        }

        public Class<?> getAnnotationType1() {
            return annotationType1;
        }

        public Class<?> getAnnotationType2() {
            return annotationType2;
        }
    }

    public static class IndistinguishableException extends Exception {

        private final Collection<Type> types;

        IndistinguishableException(Collection<Type> types) {
            this.types = types;
        }

        public Collection<Type> getTypes() {
            return types;
        }
    }

    public static class UnexpectedException extends Exception {

        private final Type type;

        private final Class<?> annotationType;

        UnexpectedException(Type type, Class<?> annotationType) {
            this.type = type;
            this.annotationType = annotationType;
        }

        public Type getType() {
            return type;
        }

        public Class<?> getAnnotationType() {
            return annotationType;
        }
    }

    @SafeVarargs
    private static Set<Class<? extends Annotation>> setOf(Class<? extends Annotation>... elements) {
        switch (elements.length) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(elements[0]);
            default:
                return new LinkedHashSet<>(Arrays.asList(elements));
        }
    }

    static {

        Map<Class<? extends Annotation>, Type> typeMap = new LinkedHashMap<>();
        Map<Type, Set<Class<? extends Annotation>>> families = new LinkedHashMap<>();
        Map<Class<? extends Annotation>, Set<Type>> inverseMap = new LinkedHashMap<>();
        Set<Class<? extends Annotation>> allTypes = new LinkedHashSet<>();

        typeMap.put(Transient.class, Type.TRANSIENT);
        typeMap.put(Id.class, Type.ID);
        typeMap.put(Version.class, Type.VERSION);
        typeMap.put(OneToOne.class, Type.ONE_TO_ONE);
        typeMap.put(ManyToOne.class, Type.MANY_TO_ONE);
        typeMap.put(OneToMany.class, Type.ONE_TO_MANY);
        typeMap.put(ManyToMany.class, Type.MANY_TO_MANY);

        families.put(Type.TRANSIENT, setOf());
        families.put(Type.ID, setOf(Column.class));
        families.put(Type.VERSION, setOf(Column.class));
        families.put(Type.BASIC, setOf(Key.class, Column.class));
        families.put(Type.EMBEDDED, setOf(Key.class, PropOverrides.class, PropOverride.class));
        families.put(Type.ONE_TO_ONE, setOf(Key.class, OnDissociate.class, JoinColumns.class, JoinColumn.class, JoinTable.class));
        families.put(Type.MANY_TO_ONE, setOf(Key.class, OnDissociate.class, JoinColumns.class, JoinColumn.class, JoinTable.class));
        families.put(Type.ONE_TO_MANY, setOf());
        families.put(Type.MANY_TO_MANY, setOf(JoinTable.class));

        for (Map.Entry<Type, Set<Class<? extends Annotation>>> e : families.entrySet()) {
            Type type = e.getKey();
            if (type.getAnnotationType() != null) {
                allTypes.add(type.getAnnotationType());
            }
            for (Class<? extends Annotation> annotationType : e.getValue()) {
                allTypes.add(annotationType);
                inverseMap
                        .computeIfAbsent(annotationType, it -> new LinkedHashSet<>())
                        .add(type);
            }
        }

        TYPE_MAP = typeMap;
        FAMILY_MAP = families;
        INVERSE_MAP = inverseMap;
        ALL_ANNOTATION_TYPES = allTypes;
    }
}
