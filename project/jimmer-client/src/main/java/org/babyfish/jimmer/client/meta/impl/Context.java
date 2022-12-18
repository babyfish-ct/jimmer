package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

class Context {

    private final Context base;

    private final Metadata.OperationParser operationParser;

    private final Metadata.ParameterParser parameterParser;

    private final Map<Class<?>, JetBrainsNullity> jetBrainsNullityMap;

    private final Location location;

    final Map<StaticObjectType.Key, StaticObjectType> staticObjectTypeMap;

    final Map<Class<?>, EnumType> enumTypeMap;

    final Map<Fetcher<?>, ImmutableObjectType> fetchedImmutableObjectTypeMap;

    final Map<ImmutableType, ImmutableObjectType> rawImmutableObjectTypeMap;

    final Map<ImmutableType, ImmutableObjectType> viewImmutableObjectTypeMap;

    private final Map<FetchByInfo, Fetcher<?>> fetcherMap;

    private final Map<TypeVariable<?>, AnnotatedType> typeVariableMap;

    private final boolean ignoreTypeVariableResolving;

    Context(
            Metadata.OperationParser operationParser,
            Metadata.ParameterParser parameterParser
    ) {
        this.base = null;
        this.operationParser = operationParser;
        this.parameterParser = parameterParser;
        this.jetBrainsNullityMap = new HashMap<>();
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
        this.jetBrainsNullityMap = base.jetBrainsNullityMap;
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
        this.jetBrainsNullityMap = base.jetBrainsNullityMap;
        this.location = base.location;
        this.staticObjectTypeMap = base.staticObjectTypeMap;
        this.enumTypeMap = base.enumTypeMap;
        this.fetchedImmutableObjectTypeMap = base.fetchedImmutableObjectTypeMap;
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
        this.ignoreTypeVariableResolving = base.ignoreTypeVariableResolving;
    }

    private Context(Context base, boolean ignoreTypeVariableResolving) {
        this.base = base;
        this.operationParser = base.operationParser;
        this.parameterParser = base.parameterParser;
        this.jetBrainsNullityMap = base.jetBrainsNullityMap;
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

    public JetBrainsNullity getJetBrainsNullity(Class<?> type) {
        return jetBrainsNullityMap.computeIfAbsent(type, JetBrainsNullity::new);
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
            return parseType(resolve((AnnotatedTypeVariable) annotatedType));
        }
        if (annotatedType instanceof AnnotatedParameterizedType) {
            System.out.println("Parameterized type: " + annotatedType);
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
        throw new IllegalArgumentException(
                "Cannot resolve the typeVariable: " +
                        typeVariable +
                        " of " +
                        typeVariable.getGenericDeclaration()
        );
    }

    private ImmutableObjectType objectType(ImmutableType type, FetchBy fetchBy) {
        if (fetchBy != null) {
            FetchByInfo info = new FetchByInfo(
                    fetchBy.ownerType() != void.class ?
                            fetchBy.ownerType() :
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
        StaticObjectType staticType = staticObjectTypeMap.get(type);
        if (staticType == null) {
            staticType = StaticObjectTypeImpl.create(this, type, typeArguments);
        }
        return staticType;
    }

    private Fetcher<?> fetcherOf(FetchByInfo info) {
        Fetcher<?> fetcher = fetcherMap.get(info);
        if (fetcher == null && !fetcherMap.containsKey(info)) {
            Field field;
            try {
                field = info.getOwnerType().getDeclaredField(info.getConstant());
            } catch (NoSuchFieldException ex) {
                throw new IllegalDocMetaException(
                        "Illegal annotation @" +
                                FetchBy.class.getName() +
                                " in " +
                                location +
                                ", there is not field \"" +
                                info.getConstant() +
                                "\" in the type \"" +
                                info.getOwnerType().getName() +
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

}
