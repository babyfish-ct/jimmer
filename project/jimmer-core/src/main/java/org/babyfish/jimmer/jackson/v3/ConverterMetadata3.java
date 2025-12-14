package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.CollectionType;
import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.lang.Generics;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConverterMetadata3 {

    private static final ClassCache<ConverterMetadata3> CACHE = new ClassCache<>(
            ConverterMetadata3::create,
            false
    );

    final Type sourceType;

    final Type targetType;

    final JavaType sourceJacksonType;

    final JavaType targetJacksonType;

    final Converter<?, ?> converter;

    private ListMetadata3 listMetadata;

    ConverterMetadata3(
            Type sourceType,
            Type targetType,
            JavaType sourceJacksonType,
            JavaType targetJacksonType,
            Converter<?, ?> converter
    ) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sourceJacksonType = sourceJacksonType;
        this.targetJacksonType = targetJacksonType;
        this.converter = converter;
    }

    public Type getSourceType() {
        return sourceType;
    }

    public Type getTargetType() {
        return targetType;
    }

    public JavaType getSourceJacksonType() {
        return sourceJacksonType;
    }

    public JavaType getTargetJacksonType() {
        return targetJacksonType;
    }

    @SuppressWarnings("unchecked")
    public <S, T> Converter<S, T> getConverter() {
        return (Converter<S, T>) converter;
    }

    public static ConverterMetadata3 of(Class<? extends Converter<?, ?>> converterClass) {
        return CACHE.get(converterClass);
    }

    public ConverterMetadata3 toListMetadata() {
        ListMetadata3 listMetadata = this.listMetadata;
        if (listMetadata == null) {
            this.listMetadata = listMetadata = new ListMetadata3();
        }
        return listMetadata;
    }

    private static ConverterMetadata3 create(Class<?> converterClass) {
        if (converterClass.getTypeParameters().length != 0) {
            throw new IllegalArgumentException(
                    "Illegal converter class \"" +
                            converterClass.getName() +
                            "\", it should not have type parameters"
            );
        }
        Type[] types = Generics.getTypeArguments(converterClass, Converter.class);
        Type sourceType = types[0];
        Type targetType = types[1];
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
        JavaType sourceJacksonType = JacksonUtils3.getJacksonType(sourceType);
        JavaType targetJacksonType = JacksonUtils3.getJacksonType(targetType);
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
        return new ConverterMetadata3(sourceType, targetType, sourceJacksonType, targetJacksonType, converter);
    }

    private class ListMetadata3 extends ConverterMetadata3 {

        public ListMetadata3() {
            super(
                    Generics.makeParameterizedType(List.class, ConverterMetadata3.this.sourceType),
                    Generics.makeParameterizedType(List.class, ConverterMetadata3.this.targetType),
                    CollectionType.construct(
                            List.class,
                            null,
                            null,
                            null,
                            ConverterMetadata3.this.sourceJacksonType
                    ),
                    CollectionType.construct(
                            List.class,
                            null,
                            null,
                            null,
                            ConverterMetadata3.this.targetJacksonType
                    ),
                    new ListConverter(ConverterMetadata3.this.converter)
            );
        }

        @Override
        public ConverterMetadata3 toListMetadata() {
            throw new IllegalStateException("The current metadata is already list metadata");
        }
    }

    private static class ListConverter implements Converter<List<?>, List<?>> {

        private final Converter<Object, Object> converter;

        @SuppressWarnings("unchecked")
        private ListConverter(Converter<?, ?> converter) {
            this.converter = (Converter<Object, Object>)converter;
        }

        @NotNull
        @Override
        public List<?> output(@NotNull List<?> value) {
            List<Object> convertedList = new ArrayList<>(value.size());
            for (Object e : value) {
                convertedList.add(converter.output(e));
            }
            return convertedList;
        }

        @NotNull
        @Override
        public List<?> input(@NotNull List<?> jsonValue) {
            List<Object> convertedList = new ArrayList<>(jsonValue.size());
            for (Object e : jsonValue) {
                convertedList.add(converter.input(e));
            }
            return convertedList;
        }
    }
}
