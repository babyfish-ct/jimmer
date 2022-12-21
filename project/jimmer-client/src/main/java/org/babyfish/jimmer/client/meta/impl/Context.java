package org.babyfish.jimmer.client.meta.impl;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.*;
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.Type;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

class Context {

    private final Context base;

    private final Metadata.OperationParser operationParser;

    private final Metadata.ParameterParser parameterParser;

    private final Map<Class<?>, JetBrainsMetadata> jetBrainsMetadataMap;

    private final Location location;

    final Map<StaticObjectType.Key, StaticObjectType> staticObjectTypeMap;

    final Map<Class<?>, EnumType> enumTypeMap;

    final Map<Fetcher<?>, ImmutableObjectType> fetchedImmutableObjectTypeMap;

    final Map<ImmutableType, ImmutableObjectType> rawImmutableObjectTypeMap;

    final Map<ImmutableType, ImmutableObjectType> viewImmutableObjectTypeMap;

    private final Map<FetchByInfo, Fetcher<?>> fetcherMap;

    private final Map<UnifiedTypeParameter, Object> typeVariableMap;

    private final boolean ignoreTypeVariableResolving;

    Context(
            Metadata.OperationParser operationParser,
            Metadata.ParameterParser parameterParser
    ) {
        this.base = null;
        this.operationParser = operationParser;
        this.parameterParser = parameterParser;
        this.jetBrainsMetadataMap = new HashMap<>();
        this.location = null;
        this.staticObjectTypeMap = new LinkedHashMap<>();
        this.enumTypeMap = new LinkedHashMap<>();
        this.rawImmutableObjectTypeMap = new LinkedHashMap<>();
        this.viewImmutableObjectTypeMap = new LinkedHashMap<>();
        this.fetchedImmutableObjectTypeMap = new LinkedHashMap<>();
        this.fetcherMap = new HashMap<>();
        this.typeVariableMap = Collections.emptyMap();
        this.ignoreTypeVariableResolving = false;
    }

    private Context(Context base, Location location) {
        this.base = base;
        this.operationParser = base.operationParser;
        this.parameterParser = base.parameterParser;
        this.jetBrainsMetadataMap = base.jetBrainsMetadataMap;
        this.location = location;
        this.staticObjectTypeMap = base.staticObjectTypeMap;
        this.enumTypeMap = base.enumTypeMap;
        this.fetchedImmutableObjectTypeMap = base.fetchedImmutableObjectTypeMap;
        this.rawImmutableObjectTypeMap = base.rawImmutableObjectTypeMap;
        this.viewImmutableObjectTypeMap = base.viewImmutableObjectTypeMap;
        this.fetcherMap = base.fetcherMap;
        this.typeVariableMap = Collections.emptyMap();
        this.ignoreTypeVariableResolving = base.ignoreTypeVariableResolving;
    }

    public Context(Context base, AnnotatedParameterizedType parameterizedType) {
        this.base = base;
        this.operationParser = base.operationParser;
        this.parameterParser = base.parameterParser;
        this.jetBrainsMetadataMap = base.jetBrainsMetadataMap;
        this.location = base.location;
        this.staticObjectTypeMap = base.staticObjectTypeMap;
        this.enumTypeMap = base.enumTypeMap;
        this.fetchedImmutableObjectTypeMap = base.fetchedImmutableObjectTypeMap;
        this.rawImmutableObjectTypeMap = base.rawImmutableObjectTypeMap;
        this.viewImmutableObjectTypeMap = base.viewImmutableObjectTypeMap;
        this.fetcherMap = base.fetcherMap;
        TypeVariable<?>[] typeVariables = ((Class<?>)((ParameterizedType)parameterizedType.getType()).getRawType()).getTypeParameters();
        AnnotatedType[] actualTypes = parameterizedType.getAnnotatedActualTypeArguments();
        Map<UnifiedTypeParameter, Object> map = new HashMap<>();
        for (int i = typeVariables.length - 1; i >= 0; --i) {
            map.put(new UnifiedTypeParameter(typeVariables[i]), actualTypes[i]);
        }
        this.typeVariableMap = map;
        this.ignoreTypeVariableResolving = base.ignoreTypeVariableResolving;
    }

    public Context(Context base, KType parameterizedType) {
        if (parameterizedType.getArguments().isEmpty()) {
            throw new IllegalArgumentException("parameterizedType must have type arguments");
        }
        this.base = base;
        this.operationParser = base.operationParser;
        this.parameterParser = base.parameterParser;
        this.jetBrainsMetadataMap = base.jetBrainsMetadataMap;
        this.location = base.location;
        this.staticObjectTypeMap = base.staticObjectTypeMap;
        this.enumTypeMap = base.enumTypeMap;
        this.fetchedImmutableObjectTypeMap = base.fetchedImmutableObjectTypeMap;
        this.rawImmutableObjectTypeMap = base.rawImmutableObjectTypeMap;
        this.viewImmutableObjectTypeMap = base.viewImmutableObjectTypeMap;
        this.fetcherMap = base.fetcherMap;
        List<KTypeParameter> typeParameters = ((KClass<?>)parameterizedType.getClassifier()).getTypeParameters();
        List<KTypeProjection> projections = parameterizedType.getArguments();
        Map<UnifiedTypeParameter, Object> map = new HashMap<>();
        for (int i = typeParameters.size() - 1; i >= 0; --i) {
            map.put(
                    new UnifiedTypeParameter(typeParameters.get(i)),
                    UnifiedTypeParameter.wrap(projections.get(i).getType())
            );
        }
        this.typeVariableMap = map;
        this.ignoreTypeVariableResolving = base.ignoreTypeVariableResolving;
    }

    private Context(Context base, boolean ignoreTypeVariableResolving) {
        this.base = base;
        this.operationParser = base.operationParser;
        this.parameterParser = base.parameterParser;
        this.jetBrainsMetadataMap = base.jetBrainsMetadataMap;
        this.location = base.location;
        this.staticObjectTypeMap = base.staticObjectTypeMap;
        this.enumTypeMap = base.enumTypeMap;
        this.fetchedImmutableObjectTypeMap = base.fetchedImmutableObjectTypeMap;
        this.rawImmutableObjectTypeMap = base.rawImmutableObjectTypeMap;
        this.viewImmutableObjectTypeMap = base.viewImmutableObjectTypeMap;
        this.fetcherMap = base.fetcherMap;
        this.typeVariableMap = base.typeVariableMap;
        this.ignoreTypeVariableResolving = ignoreTypeVariableResolving;
    }

    public Context locate(Location location) {
        return new Context(this, location);
    }

    public Location getLocation() {
        return location;
    }

    public Metadata.OperationParser getOperationParser() {
        return operationParser;
    }

    public Metadata.ParameterParser getParameterParser() {
        return parameterParser;
    }

    public JetBrainsMetadata getJetBrainsMetadata(Class<?> type) {
        return jetBrainsMetadataMap.computeIfAbsent(type, t -> new JetBrainsMetadata(type));
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
            Class<?> javaClass = (Class<?>) javaType;
            if (immutableType != null) {
                return objectType(immutableType, fetchBy);
            }
            if (javaClass.isEnum()) {
                EnumType enumType = enumTypeMap.get(javaClass);
                if (enumType == null) {
                    enumType = new EnumTypeImpl(javaClass);
                    enumTypeMap.put(javaClass, enumType);
                }
                return enumType;
            }
            SimpleType simpleType = SimpleTypeImpl.get(javaClass);
            if (simpleType != null) {
                return simpleType;
            }
            if (Collection.class.isAssignableFrom(javaClass) ||
                    Map.class.isAssignableFrom(javaClass)) {
                throw new IllegalDocMetaException(
                        "Illegal type \"" +
                                annotatedType +
                                "\" declared in " +
                                location +
                                ", collection and map must be parameterized type"
                );
            }
            if (!ignoreTypeVariableResolving && javaClass.getTypeParameters().length != 0) {
                throw new IllegalDocMetaException(
                        "Illegal type \"" +
                                annotatedType +
                                "\" declared in " +
                                location +
                                ", generic type must be parameterized type"
                );
            }
            return objectType(javaClass, null);
        }
        if (annotatedType instanceof AnnotatedWildcardType) {
            return parseType(((AnnotatedWildcardType)annotatedType).getAnnotatedUpperBounds()[0]);
        }
        if (annotatedType instanceof AnnotatedArrayType) {
            return new ArrayTypeImpl(parseType(((AnnotatedArrayType) annotatedType).getAnnotatedGenericComponentType()));
        }
        if (annotatedType instanceof AnnotatedTypeVariable) {
            if (ignoreTypeVariableResolving) {
                return new UnresolvedTypeVariableImpl(((TypeVariable<?>)annotatedType.getType()).getName());
            }
            TypeVariable<?> typeVariable = (TypeVariable<?>) annotatedType.getType();
            Object resolvedType = resolve(new UnifiedTypeParameter(typeVariable));
            if (resolvedType instanceof KType) {
                return parseKotlinType((KType) resolvedType);
            }
            return parseType((AnnotatedType) resolvedType);
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
            if (Collection.class.isAssignableFrom(rawClass)) {
                return new ArrayTypeImpl(parseType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[0]));
            }
            if (Map.class.isAssignableFrom(rawClass)) {
                return new MapTypeImpl(
                        parseType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[0]),
                        parseType(annotatedParameterizedType.getAnnotatedActualTypeArguments()[1])
                );
            }
            return new Context(this, annotatedParameterizedType).objectType(
                    rawClass,
                    Arrays.stream(annotatedParameterizedType.getAnnotatedActualTypeArguments())
                            .map(this::parseType)
                            .collect(Collectors.toList())
            );
        }
        throw new AssertionError("Internal bug: unexpected annotated type " + annotatedType);
    }

    public Map<Class<?>, StaticObjectType> getGenericTypes() {
        Set<Class<?>> classes =
                staticObjectTypeMap
                        .values()
                        .stream()
                        .filter(it -> !it.getTypeArguments().isEmpty())
                        .map(StaticObjectType::getJavaType)
                        .collect(Collectors.toSet());
        Map<Class<?>, StaticObjectType> map = new HashMap<>((classes.size() * 4 + 2) / 3);
        Context tmpContext = new Context(this, true);
        for (Class<?> clazz : classes) {
            map.put(clazz, tmpContext.objectType(clazz, null));
        }
        return map;
    }

    private Object resolve(UnifiedTypeParameter typeParameter) {
        Object resolvedType = typeVariableMap.get(typeParameter);
        if (resolvedType != null) {
            Object wrappedResolvedType = UnifiedTypeParameter.wrap(resolvedType);
            if (!(wrappedResolvedType instanceof UnifiedTypeParameter)) {
                return resolvedType;
            }
            typeParameter = (UnifiedTypeParameter) wrappedResolvedType;
        }
        if (base != null) {
            return base.resolve(typeParameter);
        }
        throw new IllegalDocMetaException("Cannot resolve " + typeParameter + " of " + location);
    }

    public Type parseKotlinType(KType type) {
        Type parsed = parseKotlinType0(type);
        if (type.isMarkedNullable()) {
            return NullableTypeImpl.of(parsed);
        }
        return parsed;
    }

    public Type parseKotlinType0(KType type) {
        FetchBy fetchBy = null;
        for (Annotation ann : type.getAnnotations()) {
            if (ann instanceof FetchBy) {
                fetchBy = (FetchBy) ann;
                break;
            }
        }
        KClass<?> kotlinClass = null;
        Class<?> javaClass = null;
        ImmutableType immutableType = null;
        if (type.getClassifier() instanceof KClass<?>) {
            kotlinClass = (KClass<?>) type.getClassifier();
            javaClass = JvmClassMappingKt.getJavaClass(kotlinClass);
            immutableType = ImmutableType.tryGet(javaClass);
        }
        if (fetchBy != null && (immutableType == null || !immutableType.isEntity())) {
            throw new IllegalDocMetaException(
                    "Illegal type \"" +
                            type +
                            "\" declared in " +
                            location +
                            ", @" +
                            FetchBy.class.getName() +
                            " can only used to decorate entity type"
            );
        }
        if (javaClass != null && type.getArguments().isEmpty()) {
            if (immutableType != null) {
                return objectType(immutableType, fetchBy);
            }
            if (javaClass.isEnum()) {
                EnumType enumType = enumTypeMap.get(javaClass);
                if (enumType == null) {
                    enumType = new EnumTypeImpl(javaClass);
                    enumTypeMap.put(javaClass, enumType);
                }
                return enumType;
            }
            SimpleType simpleType = SimpleTypeImpl.get(javaClass);
            if (simpleType != null) {
                return simpleType;
            }
            if (Collection.class.isAssignableFrom(javaClass) ||
                    Map.class.isAssignableFrom(javaClass)) {
                throw new IllegalDocMetaException(
                        "Illegal type \"" +
                                type +
                                "\" declared in " +
                                location +
                                ", collection and map must be parameterized type"
                );
            }
            if (!ignoreTypeVariableResolving && javaClass.getTypeParameters().length != 0) {
                throw new IllegalDocMetaException(
                        "Illegal type \"" +
                                type +
                                "\" declared in " +
                                location +
                                ", generic type must be parameterized type"
                );
            }
            return objectType(kotlinClass, null);
        }
        if (javaClass != null && !type.getArguments().isEmpty()) {
            List<KType> argumentTypes = new ArrayList<>();
            for (KTypeProjection projection : type.getArguments()) {
                KType argumentType = projection.getType();
                if (argumentType == null) {
                    throw new IllegalDocMetaException(
                            "Illegal type \"" +
                                    type +
                                    "\" declared in " +
                                    location +
                                    ", generic type argument cannot be star"
                    );
                }
                argumentTypes.add(argumentType);
            }
            if (Collection.class.isAssignableFrom(javaClass)) {
                return new ArrayTypeImpl(parseKotlinType(argumentTypes.get(0)));
            }
            if (Map.class.isAssignableFrom(javaClass)) {
                return new MapTypeImpl(
                        parseKotlinType(argumentTypes.get(0)),
                        parseKotlinType(argumentTypes.get(1))
                );
            }
            return new Context(this, type).objectType(
                    kotlinClass,
                    argumentTypes.stream().map(this::parseKotlinType).collect(Collectors.toList())
            );
        }
        if (type.getClassifier() instanceof KTypeParameter) {
            if (ignoreTypeVariableResolving) {
                return new UnresolvedTypeVariableImpl(((KTypeParameter) type.getClassifier()).getName());
            }
            Object resolved = resolve(new UnifiedTypeParameter((KTypeParameter) type.getClassifier()));
            if (resolved instanceof KType) {
                return parseKotlinType((KType) resolved);
            }
            return parseType((AnnotatedType) resolved);
        }
        throw new AssertionError("Internal bug: unexpected kotlin type " + type);
    }

    private ImmutableObjectType objectType(ImmutableType type, FetchBy fetchBy) {
        if (fetchBy != null) {
            Class<?> ownerType;
            try {
                ownerType = fetchBy.ownerType();
            } catch (KotlinReflectionInternalError ex) {
                // Bug of kotlin reflection
                ownerType = void.class;
            }
            FetchByInfo info = new FetchByInfo(
                    ownerType != void.class ?
                            ownerType :
                            location.getDeclaringType(),
                    fetchBy.value()
            );
            Fetcher<?> fetcher = fetcherOf(info);
            return ImmutableObjectTypeImpl.fetch(this, type, fetcher, info);
        }
        if (location.isQueryResult()) {
            return ImmutableObjectTypeImpl.view(this, type);
        }
        return ImmutableObjectTypeImpl.raw(this, type);
    }

    private StaticObjectType objectType(Class<?> type, List<Type> typeArguments) {
        if (type.isAnnotationPresent(kotlin.Metadata.class)) {
            return objectType(JvmClassMappingKt.getKotlinClass(type), typeArguments);
        }
        StaticObjectType staticType = staticObjectTypeMap.get(new StaticObjectType.Key(type, typeArguments));
        if (staticType == null) {
            staticType = StaticObjectTypeImpl.create(this, type, typeArguments);
        }
        return staticType;
    }

    private StaticObjectType objectType(KClass<?> type, List<Type> typeArguments) {
        StaticObjectType staticType = staticObjectTypeMap.get(new StaticObjectType.Key(JvmClassMappingKt.getJavaClass(type), typeArguments));
        if (staticType == null) {
            staticType = StaticObjectTypeImpl.create(this, type, typeArguments);
        }
        return staticType;
    }

    private Fetcher<?> fetcherOf(FetchByInfo info) {
        Fetcher<?> fetcher = fetcherMap.get(info);
        if (fetcher == null && !fetcherMap.containsKey(info)) {
            Ref<Fetcher<?>> fetcherRef = staticFetcherOf(info, info.getOwnerType().isAnnotationPresent(kotlin.Metadata.class));
            if (fetcherRef != null) {
                fetcher = fetcherRef.getValue();
            } else {
                fetcher = companionFetcherOf(info);
            }
            fetcherMap.put(info, fetcher);
        }
        return fetcher;
    }

    private Ref<Fetcher<?>> staticFetcherOf(FetchByInfo info, boolean allowReturnNull) {
        Field field;
        try {
            field = info.getOwnerType().getDeclaredField(info.getConstant());
        } catch (NoSuchFieldException ex) {
            if (allowReturnNull) {
                return null;
            }
            throw new IllegalDocMetaException(
                    "Illegal annotation @" +
                            FetchBy.class.getName() +
                            " in " +
                            location +
                            ", there is no field \"" +
                            info.getConstant() +
                            "\" in the type \"" +
                            info.getOwnerType().getName() +
                            "\""
            );
        }
        if (!Modifier.isStatic(field.getModifiers()) ||
                !Modifier.isFinal(field.getModifiers()) ||
                !Fetcher.class.isAssignableFrom(field.getType())
        ) {
            if (allowReturnNull) {
                return null;
            }
            throw new IllegalDocMetaException(
                    "Illegal annotation @" +
                            FetchBy.class.getName() +
                            " in " +
                            location +
                            ", the field \"" +
                            field +
                            "\" must be static and final and must return fetcher"
            );
        }
        field.setAccessible(true);
        try {
            return Ref.of((Fetcher<?>) field.get(null));
        } catch (IllegalAccessException ex) {
            throw new IllegalDocMetaException(
                    "Cannot get `" +
                            info.getConstant() +
                            "` from \"" +
                            info.getOwnerType().getName() +
                            "\""
            );
        }
    }

    private Fetcher<?> companionFetcherOf(FetchByInfo info) {
        Field companionField;
        try {
            companionField = info.getOwnerType().getDeclaredField("Companion");
        } catch (NoSuchFieldException ex) {
            companionField = null;
        }
        Object companion = null;
        Field field = null;
        if (companionField != null) {
            companionField.setAccessible(true);
            try {
                companion = companionField.get(null);
            } catch (IllegalAccessException ex) {
                // Do nothing
            }
            if (companion != null) {
                try {
                    field = companionField.getType().getDeclaredField(info.getConstant());
                } catch (NoSuchFieldException ex) {
                    // Do nothing
                }
            }
        }
        if (field == null) {
            throw new IllegalDocMetaException(
                    "Illegal annotation @" +
                            FetchBy.class.getName() +
                            " in " +
                            location +
                            ", no static of companion fetcher \"" +
                            info.getConstant() +
                            "\""
            );
        }
        if (!Fetcher.class.isAssignableFrom(field.getType())) {
            throw new IllegalDocMetaException(
                    "Illegal annotation @" +
                            FetchBy.class.getName() +
                            " in " +
                            location +
                            ", the field \"" +
                            field +
                            "\" must return fetcher"
            );
        }
        field.setAccessible(true);
        try {
            return (Fetcher<?>) field.get(companion);
        } catch (IllegalAccessException ex) {
            throw new IllegalDocMetaException(
                    "Cannot get `" +
                            info.getConstant() +
                            "` from \"" +
                            info.getOwnerType().getName() +
                            "\""
            );
        }
    }

    ImmutableObjectType getImmutableObjectType(ImmutableObjectType.Category category, ImmutableType type, Fetcher<?> fetcher) {
        switch (category) {
            case FETCH:
                return fetchedImmutableObjectTypeMap.get(fetcher);
            case VIEW:
                return viewImmutableObjectTypeMap.get(type);
            case RAW:
                return rawImmutableObjectTypeMap.get(type);
            default:
                return null;
        }
    }

    StaticObjectType getStaticObjectType(Class<?> rawType, List<Type> typeArguments) {
        return staticObjectTypeMap.get(new StaticObjectType.Key(rawType, typeArguments));
    }

    void addStaticObjectType(StaticObjectTypeImpl impl) {
        staticObjectTypeMap.put(new StaticObjectType.Key(impl.getJavaType(), impl.getTypeArguments()), impl);
    }

    void addImmutableObjectType(ImmutableObjectTypeImpl impl) {
        switch (impl.getCategory()) {
            case FETCH:
                fetchedImmutableObjectTypeMap.put(impl.getFetcher(), impl);
                break;
            case VIEW:
                viewImmutableObjectTypeMap.put(impl.getImmutableType(), impl);
                break;
            case RAW:
                rawImmutableObjectTypeMap.put(impl.getImmutableType(), impl);
                break;
        }
    }

    private static class UnifiedTypeParameter {

        private final String name;

        UnifiedTypeParameter(TypeVariable<?> typeVariable) {
            name = typeVariable.getName();
        }

        UnifiedTypeParameter(KTypeParameter typeParameter) {
            name = typeParameter.getName();
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnifiedTypeParameter that = (UnifiedTypeParameter) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return '<' + name + '>';
        }

        public static Object wrap(Object o) {
            if (o instanceof AnnotatedTypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>)((AnnotatedTypeVariable)o).getType();
                return new UnifiedTypeParameter(typeVariable);
            } if (o instanceof TypeVariable<?>) {
                return new UnifiedTypeParameter((TypeVariable<?>) o);
            } else if (o instanceof KTypeParameter) {
                return new UnifiedTypeParameter((KTypeParameter) o);
            }
            return o;
        }
    }
}
