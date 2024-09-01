package org.babyfish.jimmer.sql.runtime;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

class RuntimeExceptionTranslator implements ExceptionTranslator<Exception> {

    private static final Item[] EMPTY_ITEM_ARR = new Item[0];

    private final Item[] items;

    RuntimeExceptionTranslator(Item[] items) {
        this.items = items;
    }

    @Override
    public @Nullable Exception translate(@NotNull Exception exception, @NotNull Args args) {
        Set<Class<?>> handledTypes = new HashSet<>();
        while (true) {
            Exception translated = translate(exception, args, handledTypes);
            if (translated == exception) {
                return translated;
            }
            exception = translated;
        }
    }

    private Exception translate(Exception exception, Args args, Set<Class<?>> handledTypes) {
        Class<?> type = exception.getClass();
        for (Item item : items) {
            if (item.type.isAssignableFrom(type) && handledTypes.add(type)) {
                Exception translated = item.translator.translate(exception, args);
                if (translated != null && translated != exception) {
                    return translated;
                }
            }
        }
        return exception;
    }

    @SuppressWarnings("unchecked")
    static ExceptionTranslator<Exception> of(Collection<ExceptionTranslator<?>> translators) {
        if (translators == null) {
            return null;
        }
        List<ExceptionTranslator<?>> nonNullTranslators = new ArrayList<>();
        for (ExceptionTranslator<?> translator : translators) {
            if (translator != null) {
                nonNullTranslators.add(translator);
            }
        }
        if (nonNullTranslators.isEmpty()) {
            return null;
        }
        Set<Class<?>> scatteredExceptionTypes = new HashSet<>();
        Map<Class<?>, Item> itemMap = new HashMap<>();
        for (ExceptionTranslator<?> translator : nonNullTranslators) {
            if (translator instanceof RuntimeExceptionTranslator) {
                Item[] oldItems = ((RuntimeExceptionTranslator)translator).items;
                for (Item oldItem : oldItems) {
                    itemMap.put(oldItem.type, oldItem);
                }
            } else {
                Item item = new Item(
                        exceptionType(translator),
                        (ExceptionTranslator<Exception>) translator
                );
                if (!scatteredExceptionTypes.add(item.type)) {
                    throw new IllegalArgumentException(
                            "Repeat configuration exception translator for exception type \"" +
                                    item.type.getName() +
                                    "\" in one configuration scope"
                    );
                }
                itemMap.put(item.type, item);
            }
        }
        Item[] items = itemMap.values().toArray(EMPTY_ITEM_ARR);
        int size = items.length;
        for (int i = 0; i < size; i++) {
            for (int ii = i + 1; ii < size; ii++) {
                if (items[i].type.isAssignableFrom(items[ii].type)) {
                    Item item = items[i];
                    items[i] = items[ii];
                    items[ii] = item;
                }
            }
        }
        return new RuntimeExceptionTranslator(items);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Exception> exceptionType(ExceptionTranslator<?> translator) {
        Map<TypeVariable<?>, Type> typeMap =
                TypeUtils.getTypeArguments(translator.getClass(), ExceptionTranslator.class);
        if (typeMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Illegal exception translator type \"" +
                            translator.getClass().getName() +
                            "\", it does not specify the type argument of \"" +
                            ExceptionTranslator.class.getName() +
                            "\""
            );
        }
        Type type = typeMap.values().iterator().next();
        if (!(type instanceof Class<?>)) {
            throw new IllegalArgumentException(
                    "Illegal exception translator type \"" +
                            translator.getClass().getName() +
                            "\", it specifies the type argument of \"" +
                            ExceptionTranslator.class.getName() +
                            "\" but that type is not class type"
            );
        }
        Class<?> exceptionType = (Class<?>) type;
        if (!Exception.class.isAssignableFrom(exceptionType)) {
            throw new IllegalArgumentException(
                    "Illegal exception translator type \"" +
                            translator.getClass().getName() +
                            "\", it specifies the type argument of \"" +
                            ExceptionTranslator.class.getName() +
                            "\" as \"" +
                            exceptionType.getName() +
                            "\", but it is not derived type of \"" +
                            RuntimeException.class.getName() +
                            "\""
            );
        }
        if (exceptionType == Exception.class || exceptionType == RuntimeException.class) {
            throw new IllegalArgumentException(
                    "Illegal exception translator type \"" +
                            translator.getClass().getName() +
                            "\", it specifies the type argument of \"" +
                            ExceptionTranslator.class.getName() +
                            "\" as \"" +
                            exceptionType.getName() +
                            "\", that type is too general, please specify a more specific exception type"
            );
        }
        return (Class<? extends Exception>) exceptionType;
    }

    private static class Item {

        final Class<? extends Exception> type;

        final ExceptionTranslator<Exception> translator;

        private Item(Class<? extends Exception> type, ExceptionTranslator<Exception> translator) {
            this.type = type;
            this.translator = translator;
        }
    }
}
