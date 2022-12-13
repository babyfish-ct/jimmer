package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.lang.reflect.*;
import java.util.*;

class Context {

    private final Context base;

    private final Metadata.OperationParser operationParser;

    private final Metadata.ParameterParser parameterParser;

    private final Location location;

    final Map<java.lang.reflect.Type, ObjectType> staticObjectTypeMap;

    final Map<Class<?>, EnumType> enumTypeMap;

    final Map<ImmutableType, ImmutableObjectType> rawImmutableObjectTypeMap;

    final Map<ImmutableType, ImmutableObjectType> viewImmutableObjectTypeMap;

    private final Map<FetchByInfo, Fetcher<?>> fetcherMap;

    private final Map<TypeVariable<?>, AnnotatedType> typeVariableMap;

    Context(
            Metadata.OperationParser operationParser,
            Metadata.ParameterParser parameterParser
    ) {
        this.base = null;
        this.operationParser = operationParser;
        this.parameterParser = parameterParser;
        this.location = null;
        this.staticObjectTypeMap = new LinkedHashMap<>();
        this.enumTypeMap = new LinkedHashMap<>();
        this.rawImmutableObjectTypeMap = new LinkedHashMap<>();
        this.viewImmutableObjectTypeMap = new LinkedHashMap<>();
        this.fetcherMap = new HashMap<>();
        this.typeVariableMap = Collections.emptyMap();
    }

    private Context(Context base, Location location) {
        this.base = base;
        this.operationParser = base.operationParser;
        this.parameterParser = base.parameterParser;
        this.location = location;
        this.staticObjectTypeMap = base.staticObjectTypeMap;
        this.enumTypeMap = base.enumTypeMap;
        this.rawImmutableObjectTypeMap = base.rawImmutableObjectTypeMap;
        this.viewImmutableObjectTypeMap = base.viewImmutableObjectTypeMap;
        this.fetcherMap = base.fetcherMap;
        this.typeVariableMap = Collections.emptyMap();
    }

    private Context(Context base, AnnotatedParameterizedType parameterizedType) {
        this.base = base;
        this.operationParser = base.operationParser;
        this.parameterParser = base.parameterParser;
        this.location = base.location;
        this.staticObjectTypeMap = base.staticObjectTypeMap;
        this.enumTypeMap = base.enumTypeMap;
        this.rawImmutableObjectTypeMap = base.rawImmutableObjectTypeMap;
        this.viewImmutableObjectTypeMap = base.viewImmutableObjectTypeMap;
        this.fetcherMap = base.fetcherMap;
        TypeVariable<?>[] typeVariables = ((Class<?>)((ParameterizedType)parameterizedType.getType()).getRawType()).getTypeParameters();
        AnnotatedType[] actualTypes = parameterizedType.getAnnotatedActualTypeArguments();
        Map<TypeVariable<?>, AnnotatedType> map = new HashMap<>();
        for (int i = typeVariables.length - 1; i >= 0; --i) {
            map.put(typeVariables[i], actualTypes[i]);
        }
        this.typeVariableMap = map;
    }

    public Context locate(Location location) {
        return new Context(this, location);
    }

    public Metadata.OperationParser getOperationParser() {
        return operationParser;
    }

    public Metadata.ParameterParser getParameterParser() {
        return parameterParser;
    }

    public Type parseType(AnnotatedType annotatedType) {

        java.lang.reflect.Type javaType = annotatedType.getType();
        FetchBy fetchBy = annotatedType.getAnnotation(FetchBy.class);
        ImmutableType immutableType = null;
        if (javaType instanceof Class<?>) {
            immutableType = ImmutableType.tryGet((Class<?>) javaType);
        }
        if (fetchBy != null && (immutableType == null || !immutableType.isEntity())) {
            throw new IllegalDocMetaException(
                    "Illegal type \"" +
                            annotatedType +
                            "\" declared in " +
                            location +
                            ", @" +
                            FetchBy.class.getName() +
                            " can only used to decorate entity type"
            );
        }
        if (javaType instanceof Class<?>) {
            if (immutableType != null) {
                return objectType(immutableType, fetchBy);
            }
            if (((Class<?>)javaType).isEnum()) {
                EnumType enumType = enumTypeMap.get((Class<?>) javaType);
                if (enumType == null) {
                    enumType = new EnumTypeImpl((Class<?>) javaType);
                    enumTypeMap.put((Class<?>) javaType, enumType);
                }
                return enumType;
            }
            SimpleType simpleType = SimpleTypeImpl.get((Class<?>) javaType);
            if (simpleType != null) {
                return simpleType;
            }
            if (Iterable.class.isAssignableFrom((Class<?>) javaType) ||
                    Map.class.isAssignableFrom((Class<?>) javaType)) {
                throw new IllegalDocMetaException(
                        "Illegal type \"" +
                                annotatedType +
                                "\" declared in " +
                                location +
                                ", collection and map must be parameterized type"
                );
            }
            return objectType((Class<?>) javaType);
        }
        if (annotatedType instanceof AnnotatedWildcardType) {
            return parseType(((AnnotatedWildcardType)annotatedType).getAnnotatedUpperBounds()[0]);
        }
        if (annotatedType instanceof AnnotatedArrayType) {
            return new ArrayTypeImpl(parseType(((AnnotatedArrayType) annotatedType).getAnnotatedGenericComponentType()));
        }
        if (annotatedType instanceof AnnotatedTypeVariable) {
            return parseType(resolve((AnnotatedTypeVariable) annotatedType));
        }
        if (annotatedType instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType annotatedParameterizedType = (AnnotatedParameterizedType) annotatedType;
            ParameterizedType parameterizedType = (ParameterizedType) annotatedParameterizedType.getType();
            java.lang.reflect.Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class<?>)) {
                throw new IllegalDocMetaException(
                        "Illegal type \"" +
                                annotatedType +
                                "\" declared in " +
                                location +
                                ", the parameterized whose raw type is not class is not supported"
                );
            }
            Class<?> rawClass = (Class<?>) rawType;
            if (Iterable.class.isAssignableFrom(rawClass)) {
                return new ArrayTypeImpl(parseType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[0]));
            }
            if (Map.class.isAssignableFrom(rawClass)) {
                return new MapTypeImpl(
                        parseType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[0]),
                        parseType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[1])
                );
            }
            return new Context(this, annotatedParameterizedType).objectType(rawClass);
        }
        throw new AssertionError("Internal bug: unexpected annotated type " + annotatedType);
    }

    private AnnotatedType resolve(AnnotatedTypeVariable typeVariable) {
        AnnotatedType annotatedType = resolve0((TypeVariable<?>) typeVariable.getType());
        if (!(annotatedType instanceof AnnotatedTypeVariable)) {
            return annotatedType;
        }
        return resolve((AnnotatedTypeVariable) annotatedType);
    }

    private AnnotatedType resolve0(TypeVariable<?> typeVariable) {
        AnnotatedType resolvedType = typeVariableMap.get(typeVariable);
        if (resolvedType != null) {
            return resolvedType;
        }
        if (base != null) {
            return base.resolve0(typeVariable);
        }
        throw new IllegalDocMetaException(
                "Illegal type variable \"" +
                        typeVariable +
                        "\" declared in " +
                        location +
                        " cannot be resolved"
        );
    }

    private ImmutableObjectType objectType(ImmutableType type, FetchBy fetchBy) {
        if (fetchBy != null) {
            Fetcher<?> fetcher = fetcherOf(fetchBy);
            if (type != fetcher.getImmutableType()) {
                throw new IllegalDocMetaException(
                        "Illegal " +
                                location +
                                ", @" +
                                FetchBy.class.getName() +
                                " specifies a fetcher whose type is \"" +
                                fetcher.getImmutableType() +
                                "\", but the decorated type is \"" +
                                type +
                                "\""
                );
            }
            return ImmutableObjectTypeImpl.fetch(this, fetcher);
        }
        if (location.isQueryResult()) {
            ImmutableObjectType immutableObjectType = viewImmutableObjectTypeMap.get(type);
            if (immutableObjectType == null) {
                immutableObjectType = ImmutableObjectTypeImpl.view(this, type);
            }
            return immutableObjectType;
        }
        ImmutableObjectType immutableObjectType = rawImmutableObjectTypeMap.get(type);
        if (immutableObjectType == null) {
            immutableObjectType = ImmutableObjectTypeImpl.raw(this, type);
        }
        return immutableObjectType;
    }

    private ObjectType objectType(Class<?> type) {
        ObjectType staticType = staticObjectTypeMap.get(type);
        if (staticType == null) {
            staticType = StaticObjectTypeImpl.create(this, type);
        }
        return staticType;
    }

    private Fetcher<?> fetcherOf(FetchBy fetchBy) {
        FetchByInfo info = new FetchByInfo(
                fetchBy.ownerType() != void.class ?
                        fetchBy.ownerType() :
                        location.getDeclaringType(),
                fetchBy.value()
        );
        Fetcher<?> fetcher = fetcherMap.get(info);
        if (fetcher == null && !fetcherMap.containsKey(info)) {
            Field field;
            try {
                field = info.ownerType.getDeclaredField(info.constant);
            } catch (NoSuchFieldException ex) {
                throw new IllegalDocMetaException(
                        "Illegal annotation @" +
                                FetchBy.class.getName() +
                                " in " +
                                location +
                                ", there is not field \"" +
                                info.constant +
                                "\" in the type \"" +
                                info.ownerType.getName() +
                                "\""
                );
            }
            if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())) {
                throw new IllegalDocMetaException(
                        "Illegal annotation @" +
                                FetchBy.class.getName() +
                                " in " +
                                location +
                                ", the field \"" +
                                field +
                                "\" must be static and final"
                );
            }
            field.setAccessible(true);
            try {
                fetcher = (Fetcher<?>) field.get(null);
            } catch (IllegalAccessException ex) {
                throw new AssertionError("Internal bug", ex);
            }
            fetcherMap.put(info, fetcher);
        }
        return fetcher;
    }

    void addStaticObjectType(StaticObjectTypeImpl impl) {
        staticObjectTypeMap.put(impl.getJavaType(), impl);
    }

    void addImmutableObjectType(ImmutableObjectTypeImpl impl) {
        switch (impl.getCategory()) {
            case VIEW:
                viewImmutableObjectTypeMap.put(impl.getImmutableType(), impl);
                break;
            case RAW:
                rawImmutableObjectTypeMap.put(impl.getImmutableType(), impl);
                break;
        }
    }

    private static class FetchByInfo {

        private final Class<?> ownerType;

        private final String constant;

        FetchByInfo(Class<?> ownerType, String constant) {
            this.ownerType = ownerType;
            this.constant = constant;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FetchByInfo that = (FetchByInfo) o;
            return ownerType.equals(that.ownerType) && constant.equals(that.constant);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ownerType, constant);
        }

        @Override
        public String toString() {
            return "FetchByInfo{" +
                    "ownerType=" + ownerType +
                    ", constant='" + constant + '\'' +
                    '}';
        }
    }
}
