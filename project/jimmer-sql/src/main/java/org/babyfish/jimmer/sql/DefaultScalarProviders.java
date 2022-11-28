package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

class DefaultScalarProviders {

    private static final StaticCache<Class<?>, ScalarProvider<?, ?>> cache =
        new StaticCache<>(DefaultScalarProviders::createProvider, true);

    private DefaultScalarProviders() {}

    public static ScalarProvider<?, ?> getProvider(Class<?> type) {
        return cache.get(type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ScalarProvider<?, ?> createProvider(Class<?> type) {
        if (type.isAnnotationPresent(Immutable.class)) {
            throw new IllegalArgumentException(
                "\"" +
                    type +
                    "\" is not scalar type because it is decorated by @Immutable"
            );
        }
        if (type.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException(
                "\"" +
                    type +
                    "\" is not scalar type because it is decorated by @Entity"
            );
        }
        ScalarType scalarType = type.getAnnotation(ScalarType.class);
        EnumType enumType = type.getAnnotation(EnumType.class);
        if (scalarType != null && enumType != null) {
            throw new ModelException(
                "Illegal type \"" +
                    type +
                    "\", it cannot be decorated by @ScalarType and @EnumType"
            );
        }
        if (scalarType != null) {
            return newProvider(scalarType.value());
        }
        if (enumType != null && !type.isEnum()) {
            throw new ModelException(
                "Illegal type \"" +
                    type +
                    "\", it cannot be decorated by @EnumType because it is not enum"
            );
        }
        if (enumType != null && enumType.value() == EnumType.Strategy.ORDINAL) {
            return newEnumByIntProvider((Class<Enum>)type);
        }
        if (type.isEnum()) {
            return newEnumByStringProvider((Class<Enum>)type);
        }
        return null;
    }

    private static ScalarProvider<?, ?> newProvider(Class<? extends ScalarProvider<?, ?>> providerType) {
        try {
            return providerType.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
            throw new ModelException(
                "Cannot create instance for \"" +
                    providerType +
                    "\"",
                ex
            );
        } catch (InvocationTargetException ex) {
            throw new ModelException(
                "Cannot create instance for \"" +
                    providerType +
                    "\"",
                ex.getTargetException()
            );
        }
    }

    private static <E extends Enum<E>> ScalarProvider<E, ?> newEnumByStringProvider(Class<E> enumType) {
        return ScalarProvider.enumProviderByString(enumType, it -> {
            for (E enumValue: enumType.getEnumConstants()) {
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
                if (enumItem.ordinal() != -1) {
                    throw new ModelException(
                        "Illegal enum type \"" +
                            enumType.getName() +
                            "\", it is mapped by name, not ordinal, " +
                            "but ordinal of the @EnumItem of \"" +
                            enumField.getName() +
                            "\" is configured"
                    );
                }
                if (!enumItem.name().equals("")) {
                    it.map(enumValue, enumItem.name());
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> ScalarProvider<?, ?> newEnumByIntProvider(Class<E> enumType) {
        return ScalarProvider.enumProviderByInt(enumType, it -> {
            for (E enumValue: enumType.getEnumConstants()) {
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
                if (!enumItem.name().equals("")) {
                    throw new ModelException(
                        "Illegal enum type \"" +
                            enumType.getName() +
                            "\", it is mapped by ordinal, not name, " +
                            "but name of the @EnumItem of \"" +
                            enumField.getName() +
                            "\" is configured"
                    );
                }
                if (enumItem.ordinal() != -1) {
                    it.map(enumValue, enumItem.ordinal());
                }
            }
        });
    }
}
