package org.babyfish.jimmer.impl.converter;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

class ImmutableConverterImpl<T, Static> implements ImmutableConverter<T, Static> {

    private final ImmutableType immutableType;

    private final Class<Static> staticType;

    private final Collection<Mapping> mappings;

    private final BiConsumer<Draft, Static> draftModifier;

    ImmutableConverterImpl(
            ImmutableType immutableType,
            Class<Static> staticType,
            Collection<Mapping> mappings,
            BiConsumer<Draft, Static> draftModifier
    ) {
        this.immutableType = immutableType;
        this.staticType = staticType;
        this.mappings = mappings;
        this.draftModifier = draftModifier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convert(Static staticObj) {
        if (staticObj == null) {
            return null;
        }
        if (!(staticType.isAssignableFrom(staticObj.getClass()))) {
            throw new IllegalArgumentException(
                    "`staticObj` is not instance whose type is \"" + staticType.getName() + "\""
            );
        }
        return (T) Internal.produce(immutableType, null, draft -> {
            for (Mapping mapping : mappings) {
                Predicate<Object> cond = (Predicate<Object>) mapping.cond;
                if (cond == null || cond.test(staticObj)) {
                    Object value = mapping.methodHandle.invoke(staticObj);
                    if (value != null && mapping.valueConverter != null) {
                        value = mapping.valueConverter.apply(value);
                    }
                    ((DraftSpi) draft).__set(mapping.propId, value);
                }
            }
            if (draftModifier != null) {
                draftModifier.accept((Draft)draft, staticObj);
            }
        });
    }

    static class Mapping {

        final Predicate<?> cond;

        final int propId;

        final MethodHandle methodHandle;

        final Function<Object, Object> valueConverter;

        private Mapping(
                Predicate<?> cond,
                ImmutableProp prop,
                MethodHandle methodHandle,
                Function<Object, Object> valueConverter
        ) {
            this.cond = cond;
            this.propId = prop.getId();
            this.methodHandle = methodHandle;
            this.valueConverter = valueConverter;
        }

        public static Mapping create(
                Predicate<?> cond,
                ImmutableProp prop,
                Method method,
                Function<Object, Object> valueConverter,
                boolean autoMapping
        ) {
            Class<?> propType;
            boolean isList = prop.isScalarList() || prop.isReferenceList(TargetLevel.OBJECT);
            if (isList) {
                propType = List.class;
            } else {
                propType = prop.getElementClass();
            }
            if (valueConverter == null) {
                if (propType != method.getReturnType()) {
                    throw new IllegalArgumentException(
                            "Cannot " +
                                    (autoMapping ? " automatically " : "") +
                                    "map \"" +
                                    prop +
                                    "\" to \"" +
                                    method +
                                    "\" without value converter, the return type of jimmer property is \"" +
                                    propType.getName() +
                                    "\" but the return type of the method of static type is \"" +
                                    method.getReturnType().getName() +
                                    "\""
                    );
                }
                if (isList) {
                    Type type = method.getGenericReturnType();
                    if (!(type instanceof ParameterizedType)) {
                        throw new IllegalArgumentException(
                                "Cannot " +
                                        (autoMapping ? " automatically " : "") +
                                        "map \"" +
                                        prop +
                                        "\" to \"" +
                                        method +
                                        "\" without value converter, the jimmer property is list but the return type " +
                                        "of the method of static type is not generic type"
                        );
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type elementType = parameterizedType.getActualTypeArguments()[0];
                    if (!(elementType instanceof Class<?>) || !Classes.matches((Class<?>) elementType, prop.getElementClass())) {
                        throw new IllegalArgumentException(
                                "Cannot " +
                                        (autoMapping ? " automatically " : "") +
                                        "map \"" +
                                        prop +
                                        "\" to \"" +
                                        method +
                                        "\" without value converter, the list element type of jimmer property is \"" +
                                        prop.getElementClass().getName() +
                                        "\" but the list element of return type of the method of static type is \"" +
                                        elementType +
                                        "\""
                        );
                    }
                }
            }
            MethodHandle handle;
            try {
                handle = MethodHandles.lookup().findVirtual(
                        method.getDeclaringClass(),
                        method.getName(),
                        MethodType.methodType(method.getReturnType())
                );
            } catch (NoSuchMethodException | IllegalAccessException ex) {
                throw new AssertionError("Internal bug: " + ex.getMessage(), ex);
            }
            return new Mapping(cond, prop, handle, valueConverter);
        }
    }
}
