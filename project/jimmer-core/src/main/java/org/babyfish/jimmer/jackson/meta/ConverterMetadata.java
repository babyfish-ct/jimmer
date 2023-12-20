package org.babyfish.jimmer.jackson.meta;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.impl.JacksonUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConverterMetadata {

    private static final ClassCache<ConverterMetadata> CACHE = new ClassCache<>(
            ConverterMetadata::create,
            false
    );

    final Type sourceType;

    final Type targetType;

    final JavaType targetJacksonType;

    final Converter<?, ?> converter;

    private ListMetadata listMetadata;

    ConverterMetadata(Type sourceType, Type targetType, JavaType targetJacksonType, Converter<?, ?> converter) {
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

    @SuppressWarnings("unchecked")
    public <S, T> Converter<S, T> getConverter() {
        return (Converter<S, T>) converter;
    }

    public static ConverterMetadata of(Class<? extends Converter<?, ?>> converterClass) {
        return CACHE.get(converterClass);
    }

    public ConverterMetadata toListMetadata() {
        ListMetadata listMetadata = this.listMetadata;
        if (listMetadata == null) {
            this.listMetadata = listMetadata = new ListMetadata();
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

    private class ListMetadata extends ConverterMetadata {

        public ListMetadata() {
            super(
                    TypeUtils.parameterize(List.class, ConverterMetadata.this.sourceType),
                    TypeUtils.parameterize(List.class, ConverterMetadata.this.targetType),
                    CollectionType.construct(
                            List.class,
                            null,
                            null,
                            null,
                            ConverterMetadata.this.targetJacksonType
                    ),
                    new ListConverter(ConverterMetadata.this.converter)
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
        private ListConverter(Converter<?, ?> converter) {
            this.converter = (Converter<Object, Object>)converter;
        }

        @Override
        public List<?> output(List<?> value) {
            List<Object> convertedList = new ArrayList<>(value.size());
            for (Object e : value) {
                convertedList.add(converter.output(e));
            }
            return convertedList;
        }

        @Override
        public List<?> input(List<?> jsonValue) {
            List<Object> convertedList = new ArrayList<>(jsonValue.size());
            for (Object e : jsonValue) {
                convertedList.add(converter.input(e));
            }
            return convertedList;
        }
    }
}
