package org.babyfish.jimmer.jackson.meta;

import com.fasterxml.jackson.databind.JavaType;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.impl.JacksonUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

public class ConverterMetadata {

    private static final StaticCache<Class<?>, ConverterMetadata> CACHE = new StaticCache<>(
            ConverterMetadata::create,
            false
    );

    private final Type sourceType;

    private final Type targetType;

    private final JavaType targetJacksonType;

    private final Converter<?, ?> converter;

    public ConverterMetadata(Type sourceType, Type targetType, JavaType targetJacksonType, Converter<?, ?> converter) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.targetJacksonType = targetJacksonType;
        this.converter = converter;
    }

    public Type getSourceType() {
        return sourceType;
    }

    public Type getTargetType() {
        return targetType;
    }

    public JavaType getTargetJacksonType() {
        return targetJacksonType;
    }

    public Converter<?, ?> getConverter() {
        return converter;
    }

    public static ConverterMetadata of(Class<? extends Converter<?, ?>> converterClass) {
        return CACHE.get(converterClass);
    }

    private static ConverterMetadata create(Class<?> converterClass) {
        if (converterClass.getTypeParameters().length != 0) {
            throw new IllegalArgumentException(
                    "Illegal converter class \"" +
                            converterClass.getName() +
                            "\", it should not have type parameters"
            );
        }
        Map<TypeVariable<?>, Type> typeMap = TypeUtils.getTypeArguments(converterClass, Converter.class);
        Type sourceType = typeMap.get(Converter.class.getTypeParameters()[0]);
        Type targetType = typeMap.get(Converter.class.getTypeParameters()[1]);
        if (sourceType == null || targetType == null) {
            throw new IllegalArgumentException(
                    "Illegal converter class \"" +
                            converterClass.getName() +
                            "\", it does not specify type arguments for \"" +
                            Converter.class.getName() +
                            "\""
            );
        }
        Constructor<?> constructor;
        JavaType jacksonType = JacksonUtils.getJacksonType(targetType);
        try {
            constructor = converterClass.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(
                    "Illegal converter class \"" +
                            converterClass.getName() +
                            "\", it does not support default constructor"
            );
        }
        Converter<?, ?> converter;
        try {
            converter = (Converter<?, ?>) constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new IllegalArgumentException(
                    "Illegal converter class \"" +
                            converterClass.getName() +
                            "\", cannot create instance for it",
                    ex
            );
        } catch (InvocationTargetException ex) {
            throw new IllegalArgumentException(
                    "Illegal converter class \"" +
                            converterClass.getName() +
                            "\", cannot create instance for it",
                    ex.getTargetException()
            );
        }
        return new ConverterMetadata(sourceType, targetType, jacksonType, converter);
    }
}
