package org.babyfish.jimmer;

import org.babyfish.jimmer.impl.converter.ImmutableConverterBuilderImpl;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.jetbrains.annotations.NotNull;

import java.util.function.*;

public interface ImmutableConverter<T, Static> {

    @NotNull T convert(Static staticObj);

    /**
     * Only for java, kotlin developers should use `newImmutableConverter`
     * @param <T>
     * @param <Static>
     */
    static <T, Static> Builder<T, Static> newBuilder(Class<T> immutableType, Class<Static> staticType) {
        return new ImmutableConverterBuilderImpl<>(immutableType, staticType);
    }

    /**
     * Only for java, kotlin developers should use `newImmutableConverter`
     * @param <T>
     * @param <Static>
     */
    interface Builder<T, Static> {

        default Builder<T, Static> map(TypedProp<?, ?> prop) {
            return map(prop.unwrap(), prop.unwrap().getName(), null);
        }

        default Builder<T, Static> map(
                TypedProp<?, ?> prop,
                String staticPropName
        ) {
            return map(prop.unwrap(), staticPropName, null);
        }

        @SuppressWarnings("unchecked")
        default <Y> Builder<T, Static> map(
                TypedProp<T, Y> prop,
                Consumer<Mapping<Static, ?, Y>> block
        ) {
            return map(
                    prop.unwrap(),
                    prop.unwrap().getName(),
                    (Consumer<Mapping<Static, ?, ?>>)(Consumer<?>)block
            );
        }

        @SuppressWarnings("unchecked")
        default <Y> Builder<T, Static> map(
                TypedProp<T, Y> prop,
                String staticPropName,
                Consumer<Mapping<Static, ?, Y>> block
        ) {
            return map(
                    prop.unwrap(),
                    staticPropName,
                    (Consumer<Mapping<Static, ?, ?>>)(Consumer<?>)block
            );
        }

        Builder<T, Static> map(
                ImmutableProp prop, 
                String staticPropName, 
                Consumer<Mapping<Static, ?, ?>> block
        );

        default Builder<T, Static> mapList(TypedProp.Multiple<?, ?> prop) {
            return mapList(prop.unwrap(), prop.unwrap().getName(), null);
        }

        default Builder<T, Static> mapList(
                TypedProp.Multiple<?, ?> prop,
                String staticPropName
        ) {
            return mapList(prop.unwrap(), staticPropName, null);
        }

        @SuppressWarnings("unchecked")
        default <Y> Builder<T, Static> mapList(
                TypedProp.Multiple<T, Y> prop,
                Consumer<ListMapping<Static, ?, Y>> block
        ) {
            return mapList(
                    prop.unwrap(),
                    prop.unwrap().getName(),
                    (Consumer<ListMapping<Static, ?, ?>>)(Consumer<?>)block
            );
        }

        @SuppressWarnings("unchecked")
        default <Y> Builder<T, Static> mapList(
                TypedProp.Multiple<T, Y> prop,
                String staticPropName, 
                Consumer<ListMapping<Static, ?, Y>> block
        ) {
            return mapList(
                    prop.unwrap(),
                    staticPropName,
                    (Consumer<ListMapping<Static, ?, ?>>)(Consumer<?>)block
            );
        }

        Builder<T, Static> mapList(
                ImmutableProp prop,
                String staticPropName,
                Consumer<ListMapping<Static, ?, ?>> block
        );

        default Builder<T, Static> unmap(TypedProp<T, ?> prop) {
            return unmap(prop.unwrap());
        }

        Builder<T, Static> unmap(ImmutableProp ... props);

        default Builder<T, Static> autoMapOtherScalars() {
            return autoMapOtherScalars(false);
        }

        Builder<T, Static> autoMapOtherScalars(boolean partial);

        Builder<T, Static> setDraftModifier(BiConsumer<Draft, Static> modifier);

        ImmutableConverter<T, Static> build();
    }

    interface Mapping<Static, X, Y> {

        Mapping<Static, X, Y> useIf(Predicate<Static> cond);

        Mapping<Static, X, Y> valueConverter(Function<X, Y> valueConverter);

        Mapping<Static, X, Y> immutableValueConverter(ImmutableConverter<Y, X> valueConverter);

        Mapping<Static, X, Y> defaultValue(Y defaultValue);

        Mapping<Static, X, Y> defaultValue(Supplier<Y> defaultValueSupplier);
    }

    interface ListMapping<Static, X, Y> {

        ListMapping<Static, X, Y> useIf(Predicate<Static> cond);

        ListMapping<Static, X, Y> elementConverter(Function<X, Y> elementConverter);

        ListMapping<Static, X, Y> immutableValueConverter(ImmutableConverter<Y, X> elementConverter);

        ListMapping<Static, X, Y> defaultElement(Y defaultElement);

        ListMapping<Static, X, Y> defaultElement(Supplier<Y> defaultValueSupplier);
    }
}
