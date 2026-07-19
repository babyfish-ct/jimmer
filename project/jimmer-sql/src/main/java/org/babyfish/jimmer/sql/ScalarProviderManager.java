package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.impl.util.PropCache;
import org.babyfish.jimmer.jackson.codec.*;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.ScalarTypeStrategy;
import org.babyfish.jimmer.sql.runtime.AbstractScalarProvider;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.PropScalarProviderFactory;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

import static org.babyfish.jimmer.sql.ScalarProviderUtils.getSqlType;

class ScalarProviderManager implements ScalarTypeStrategy {

    private static final Set<Class<?>> GENERIC_TYPES;

    private final ClassCache<ScalarProvider<?, ?>> typeScalarProviderCache =
            new ClassCache<>(this::createProvider, true);

    private final PropCache<ScalarProvider<?, ?>> propScalarProviderCache =
            new PropCache<>(this::createProvider, true);

    private final Map<Class<?>, ScalarProvider<?, ?>> customizedTypeScalarProviderMap;

    private final Map<ImmutableProp, ScalarProvider<?, ?>> customizedPropScalarProviderMap;

    private final PropScalarProviderFactory propScalarProviderFactory;

    private final JsonCodec<?> serializedJsonCodec;

    private final Map<Class<?>, JsonCodec> serializedTypeJsonCodecMap;

    private final Map<ImmutableProp, JsonCodec> serializedPropJsonCodecMap;

    private final Function<ImmutableProp, ScalarProvider<?, ?>> defaultJsonProviderCreator;

    private final EnumType.Strategy defaultEnumStrategy;

    private final Dialect dialect;

    ScalarProviderManager(
            Map<Class<?>, ScalarProvider<?, ?>> customizedTypeScalarProviderMap,
            Map<ImmutableProp, ScalarProvider<?, ?>> customizedPropScalarProviderMap,
            PropScalarProviderFactory propScalarProviderFactory,
            JsonCodec<?> serializedJsonCodec,
            Map<Class<?>, JsonCodec<?>> serializedTypeJsonCodecMap,
            Map<ImmutableProp, JsonCodec<?>> serializedPropJsonCodecMap,
            Function<ImmutableProp, ScalarProvider<?, ?>> defaultJsonProviderCreator,
            EnumType.Strategy defaultEnumStrategy,
            Dialect dialect
    ) {
        this.customizedTypeScalarProviderMap = new HashMap<>(customizedTypeScalarProviderMap);
        this.customizedPropScalarProviderMap = new HashMap<>(customizedPropScalarProviderMap);
        this.propScalarProviderFactory = propScalarProviderFactory;
        this.serializedJsonCodec = serializedJsonCodec;
        this.serializedTypeJsonCodecMap = new HashMap<>(serializedTypeJsonCodecMap);
        this.serializedPropJsonCodecMap = new HashMap<>(serializedPropJsonCodecMap);
        this.defaultJsonProviderCreator = defaultJsonProviderCreator;
        this.defaultEnumStrategy = defaultEnumStrategy;
        this.dialect = dialect;
    }

    @Override
    public Class<?> getOverriddenSqlType(ImmutableProp prop) {
        ScalarProvider<?, ?> provider = getProvider(prop);
        return provider == null ? null : getSqlType(provider, dialect);
    }

    public ScalarProvider<?, ?> getProvider(ImmutableProp prop) {
        return propScalarProviderCache.get(prop.toOriginal());
    }

    public ScalarProvider<?, ?> getProvider(Class<?> type) {
        return typeScalarProviderCache.get(type);
    }

    private ScalarProvider<?, ?> createProvider(ImmutableProp prop) {
        ScalarProvider<?, ?> provider = customizedPropScalarProvider(prop);
        if (provider != null) {
            return provider;
        }
        if (propScalarProviderFactory != null) {
            provider = propScalarProviderFactory.createScalarProvider(prop);
            if (provider != null) {
                return provider;
            }
        }

        if (prop.getReturnClass() == UUID.class) {
            Column column = prop.getAnnotation(Column.class);
            if (column != null && !column.sqlType().isEmpty()) {
                switch (column.sqlType().toLowerCase()) {
                    case "char":
                    case "nchar":
                    case "varchar":
                    case "nvarchar":
                    case "varchar2":
                    case "nvarchar2":
                    case "text":
                        return ScalarProvider.uuidByString();
                    case "binary":
                    case "varbinary":
                    case "bytea":
                    case "byte[]":
                        return ScalarProvider.uuidByByteArray();
                }
            }
        }

        Serialized serialized = prop.getAnnotation(Serialized.class);
        if (serialized == null) {
            return typeScalarProviderCache.get(prop.getReturnClass());
        }
        if (defaultJsonProviderCreator != null) {
            return defaultJsonProviderCreator.apply(prop);
        }
        JsonCodec<?> serializedPropJsonCodec = serializedPropJsonCodec(prop);
        JsonCodec<?> jsonCodec = serializedPropJsonCodec != null ? serializedPropJsonCodec : serializedJsonCodec;
        return createJsonProvider(prop.getReturnClass(), tf -> jacksonType(tf, prop.getGenericType()), jsonCodec);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ScalarProvider<?, ?> createProvider(Class<?> type) {
        if (DbLiteral.class.isAssignableFrom(type)) {
            return null;
        }
        ScalarProvider<?, ?> provider = customizedTypeScalarProviderMap.get(type);
        if (provider != null) {
            return provider;
        }

        EnumType enumType = type.getAnnotation(EnumType.class);
        Serialized serialized = type.getAnnotation(Serialized.class);
        if (enumType != null && serialized != null) {
            throw new ModelException(
                    "Illegal type \"" +
                            type +
                            "\", it cannot be decorated by both @" +
                            EnumType.class.getName() +
                            " and @" +
                            Serialized.class.getName()
            );
        }
        if (enumType != null && !type.isEnum()) {
            throw new ModelException(
                    "Illegal type \"" +
                            type +
                            "\", it cannot be decorated by @EnumType because it is not enum"
            );
        }
        if (enumType != null && enumType.value() == EnumType.Strategy.ORDINAL) {
            return newEnumByIntProvider((Class<Enum>) type);
        }
        if (enumType != null && enumType.value() == EnumType.Strategy.NAME) {
            return newEnumByStringProvider((Class<Enum>) type);
        }
        if (type.isEnum()) {
            if (defaultEnumStrategy == EnumType.Strategy.ORDINAL) {
                return newEnumByIntProvider((Class<Enum>) type);
            }
            return newEnumByStringProvider((Class<Enum>) type);
        }

        if (serialized != null) {
            JsonCodec<?> serializedTypeJsonCodec = serializedTypeJsonCodec(type);
            JsonCodec<?> jsonCodec = serializedTypeJsonCodec != null ? serializedTypeJsonCodec : serializedJsonCodec;
            return createJsonProvider(type, tf -> tf.constructType(type), jsonCodec);
        }

        return null;
    }

    private <E extends Enum<E>> ScalarProvider<E, ?> newEnumByStringProvider(Class<E> enumType) {
        return ScalarProvider.enumProviderByString(enumType, it -> {
            for (E enumValue : enumType.getEnumConstants()) {
                Field enumField;
                try {
                    enumField = enumType.getField(enumValue.name());
                } catch (NoSuchFieldException ex) {
                    throw new AssertionError("Internal bug", ex);
                }
                EnumItem enumItem = enumField.getAnnotation(EnumItem.class);
                if (enumItem == null) {
                    break;
                }
                if (enumItem.ordinal() != -892374651) {
                    throw new ModelException(
                            "Illegal enum type \"" +
                                    enumType.getName() +
                                    "\", it is mapped by name, not ordinal, " +
                                    "but ordinal of the @EnumItem of \"" +
                                    enumField.getName() +
                                    "\" is configured"
                    );
                }
                if (!enumItem.name().isEmpty()) {
                    it.map(enumValue, enumItem.name());
                }
            }
        });
    }

    private <E extends Enum<E>> ScalarProvider<?, ?> newEnumByIntProvider(Class<E> enumType) {
        return ScalarProvider.enumProviderByInt(enumType, it -> {
            for (E enumValue : enumType.getEnumConstants()) {
                Field enumField;
                try {
                    enumField = enumType.getField(enumValue.name());
                } catch (NoSuchFieldException ex) {
                    throw new AssertionError("Internal bug", ex);
                }
                EnumItem enumItem = enumField.getAnnotation(EnumItem.class);
                if (enumItem == null) {
                    break;
                }
                if (!enumItem.name().isEmpty()) {
                    throw new ModelException(
                            "Illegal enum type \"" +
                                    enumType.getName() +
                                    "\", it is mapped by ordinal, not name, " +
                                    "but name of the @EnumItem of \"" +
                                    enumField.getName() +
                                    "\" is configured"
                    );
                }
                if (enumItem.ordinal() != -892374651) {
                    it.map(enumValue, enumItem.ordinal());
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private ScalarProvider<?, String> createJsonProvider(Class<?> type, TypeCreator typeCreator, JsonCodec<?> jsonCodec) {
        return new AbstractScalarProvider<Object, String>(
                (Class<Object>) type,
                String.class
        ) {
            final JsonReader<?> reader = jsonCodec.readerFor(typeCreator);
            final JsonWriter writer = jsonCodec.writer();

            @Override
            public @NotNull Object toScalar(@NotNull String sqlValue) throws Exception {
                return reader.read(sqlValue);
            }

            @Override
            public @NotNull String toSql(@NotNull Object scalarValue) throws Exception {
                return writer.writeAsString(scalarValue);
            }

            @Override
            public boolean isJsonScalar() {
                return true;
            }

            @Override
            public String toString() {
                return "JacksonScalarProvider";
            }
        };
    }

    private ScalarProvider<?, ?> customizedPropScalarProvider(ImmutableProp prop) {
        ScalarProvider<?, ?> provider = customizedPropScalarProviderMap.get(prop);
        if (provider != null) {
            return provider;
        }
        for (ImmutableType superType : prop.getDeclaringType().getSuperTypes()) {
            ImmutableProp superProp = superType.getProps().get(prop.getName());
            if (superProp == null) {
                continue;
            }
            ScalarProvider<?, ?> superProvider = customizedPropScalarProvider(superProp);
            if (superProvider == null) {
                continue;
            }
            if (provider != null) {
                throw new ModelException(
                        "Cannot get the customized property scalar property of \"" +
                                prop +
                                "\", because there are conflict configurations in super properties"
                );
            }
            provider = superProvider;
        }
        return provider;
    }

    private JsonCodec<?> serializedTypeJsonCodec(Class<?> type) {
        JsonCodec<?> jsonCodec = serializedTypeJsonCodecMap.get(type);
        if (jsonCodec != null) {
            return jsonCodec;
        }
        Class<?> superType = type.isInterface() ? Object.class : type.getSuperclass();
        if (superType != null) {
            jsonCodec = serializedTypeJsonCodec(superType);
        }
        for (Class<?> superItfType : type.getInterfaces()) {
            JsonCodec<?> superJsonCodec = serializedTypeJsonCodec(superItfType);
            if (superJsonCodec == null) {
                continue;
            }
            if (jsonCodec != null && jsonCodec != superJsonCodec) {
                throw new ModelException(
                        "Cannot get the serialized json codec of \"" +
                                type.getName() +
                                "\", because there are conflict configurations in super types"
                );
            }
            jsonCodec = superJsonCodec;
        }
        return jsonCodec;
    }

    private JsonCodec<?> serializedPropJsonCodec(ImmutableProp prop) {
        JsonCodec<?> jsonCodec = serializedPropJsonCodecMap.get(prop);
        if (jsonCodec != null) {
            return jsonCodec;
        }
        for (ImmutableType superType : prop.getDeclaringType().getSuperTypes()) {
            ImmutableProp superProp = superType.getProps().get(prop.getName());
            if (superProp == null) {
                continue;
            }
            JsonCodec<?> superJsonCodec = serializedPropJsonCodec(superProp);
            if (superJsonCodec == null) {
                continue;
            }
            if (jsonCodec != null && jsonCodec != superJsonCodec) {
                throw new ModelException(
                        "Cannot get the serialized json codec of \"" +
                                prop +
                                "\", because there are conflict configurations in super properties"
                );
            }
            jsonCodec = superJsonCodec;
        }
        return serializedTypeJsonCodec(prop.getReturnClass());
    }

    @SuppressWarnings("unchecked")
    private static <JT extends Type> JT jacksonType(JsonTypeFactory<JT> typeFactory, Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class<?>) || !GENERIC_TYPES.contains(rawType)) {
                throw new IllegalArgumentException(
                        "Generic type must be one of " + GENERIC_TYPES
                );
            }
            Class<?> rawClass = (Class<?>) rawType;
            if (Map.class.isAssignableFrom(rawClass)) {
                return typeFactory.constructMapType(
                        Map.class,
                        jacksonType(typeFactory, parameterizedType.getActualTypeArguments()[0]),
                        jacksonType(typeFactory, parameterizedType.getActualTypeArguments()[1]));
            }
            return typeFactory.constructCollectionType(
                    (Class<? extends Collection<?>>) rawClass,
                    jacksonType(typeFactory, parameterizedType.getActualTypeArguments()[0])
            );
        } else if (type instanceof Class<?>) {
            if (GENERIC_TYPES.contains(type)) {
                throw new IllegalArgumentException(
                        "\"" +
                                type +
                                "\" does not have generic arguments"
                );
            }
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return typeFactory.constructArrayType(jacksonType(typeFactory, clazz.getComponentType()));
            }
            return typeFactory.constructType(clazz);
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            return jacksonType(typeFactory, wildcardType.getLowerBounds()[0]);
        } else if (type instanceof TypeVariable<?>) {
            throw new IllegalArgumentException("type variable is not allowed");
        } else if (type instanceof GenericArrayType) {
            throw new IllegalArgumentException("generic array is not allowed");
        } else {
            throw new IllegalArgumentException("Unexpected type: " + type.getClass().getName());
        }
    }

    static {
        Set<Class<?>> genericTypes = new HashSet<>();
        genericTypes.add(Iterable.class);
        genericTypes.add(Collection.class);
        genericTypes.add(List.class);
        genericTypes.add(Set.class);
        genericTypes.add(SortedSet.class);
        genericTypes.add(NavigableSet.class);
        genericTypes.add(Map.class);
        genericTypes.add(SortedMap.class);
        genericTypes.add(NavigableMap.class);
        GENERIC_TYPES = genericTypes;
    }
}
