package org.babyfish.jimmer.jackson;

import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.lang.Generics;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConverterMetadata {

    private static final ClassCache<ConverterMetadata> CACHE = new ClassCache<>(
            ConverterMetadata::create,
            false
    );

    final Type sourceType;

    final Type targetType;

    final Converter<?, ?> converter;

    private ListConverterMetadata listMetadata;

    ConverterMetadata(
            Type sourceType,
            Type targetType,
            Converter<?, ?> converter
    ) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.converter = converter;
    }

    public Type getSourceType() {
        return sourceType;
    }

    public Type getTargetType() {
        return targetType;
    }

    @SuppressWarnings("unchecked")
    public <S, T> Converter<S, T> getConverter() {
        return (Converter<S, T>) converter;
    }

    public static ConverterMetadata of(Class<? extends Converter<?, ?>> converterClass) {
        return CACHE.get(converterClass);
    }

    public ConverterMetadata toListMetadata() {
        ListConverterMetadata listMetadata = this.listMetadata;
        if (listMetadata == null) {
            this.listMetadata = listMetadata = new ListConverterMetadata(this);
        }
        return listMetadata;
    }

    private static ConverterMetadata create(Class<?> converterClass) {
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
        return new ConverterMetadata(sourceType, targetType, converter);
    }

    private static class ListConverterMetadata extends ConverterMetadata {
        ListConverterMetadata(ConverterMetadata converterMetadata) {
            super(
                    Generics.makeParameterizedType(List.class, converterMetadata.sourceType),
                    Generics.makeParameterizedType(List.class, converterMetadata.targetType),
                    new ListConverter(converterMetadata.converter)
            );
        }

        @Override
        public ConverterMetadata toListMetadata() {
            throw new IllegalStateException("The current metadata is already list metadata");
        }
    }

    private static class ListConverter implements Converter<List<?>, List<?>> {
        private final Converter<Object, Object> converter;

        @SuppressWarnings("unchecked")
        public ListConverter(Converter<?, ?> converter) {
            this.converter = (Converter<Object, Object>) converter;
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
