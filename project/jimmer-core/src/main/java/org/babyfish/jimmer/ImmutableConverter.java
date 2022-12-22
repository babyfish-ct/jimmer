package org.babyfish.jimmer;

import org.babyfish.jimmer.impl.converter.ImmutableConverterBuilderImpl;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.*;

public interface ImmutableConverter<Dynamic, Static> {

    @NotNull
    Dynamic convert(Static staticObj);

    /**
     * Only for java, kotlin developers should use `newImmutableConverter`
     * @param <Dynamic>
     * @param <Static>
     */
    static <Dynamic, Static> Builder<Dynamic, Static> forMethods(
            Class<Dynamic> immutableType,
            Class<Static> staticType
    ) {
        return new ImmutableConverterBuilderImpl<>(
                immutableType,
                staticType,
                false
        );
    }

    static <Dynamic, Static> Builder<Dynamic, Static> forFields(
            Class<Dynamic> immutableType,
            Class<Static> staticType
    ) {
        return new ImmutableConverterBuilderImpl<>(immutableType, staticType, true);
    }

    /**
     * Only for java, kotlin developers should use `newImmutableConverter`
     * @param <Dynamic>
     * @param <Static>
     */
    interface Builder<Dynamic, Static> {

        default Builder<Dynamic, Static> map(TypedProp<?, ?> prop) {
            return map(prop.unwrap(), prop.unwrap().getName(), null);
        }

        default Builder<Dynamic, Static> map(
                TypedProp<?, ?> prop,
                String staticPropName
        ) {
            return map(prop.unwrap(), staticPropName, null);
        }

        @SuppressWarnings("unchecked")
        default <DynamicProp> Builder<Dynamic, Static> map(
                TypedProp<Dynamic, DynamicProp> prop,
                Consumer<Mapping<Static, DynamicProp>> block
        ) {
            return map(
                    prop.unwrap(),
                    prop.unwrap().getName(),
                    (Consumer<Mapping<Static, ?>>)(Consumer<?>)block
            );
        }

        @SuppressWarnings("unchecked")
        default <DynamicProp> Builder<Dynamic, Static> map(
                TypedProp<Dynamic, DynamicProp> prop,
                String staticPropName,
                Consumer<Mapping<Static, DynamicProp>> block
        ) {
            return map(
                    prop.unwrap(),
                    staticPropName,
                    (Consumer<Mapping<Static, ?>>)(Consumer<?>)block
            );
        }

        Builder<Dynamic, Static> map(
                ImmutableProp prop, 
                String staticPropName, 
                Consumer<Mapping<Static, ?>> block
        );

        default Builder<Dynamic, Static> mapList(TypedProp.Multiple<?, ?> prop) {
            return mapList(prop.unwrap(), prop.unwrap().getName(), null);
        }

        default Builder<Dynamic, Static> mapList(
                TypedProp.Multiple<?, ?> prop,
                String staticPropName
        ) {
            return mapList(prop.unwrap(), staticPropName, null);
        }

        @SuppressWarnings("unchecked")
        default <DynamicProp> Builder<Dynamic, Static> mapList(
                TypedProp.Multiple<Dynamic, DynamicProp> prop,
                Consumer<ListMapping<Static, DynamicProp>> block
        ) {
            return mapList(
                    prop.unwrap(),
                    prop.unwrap().getName(),
                    (Consumer<ListMapping<Static, ?>>)(Consumer<?>)block
            );
        }

        @SuppressWarnings("unchecked")
        default <DynamicProp> Builder<Dynamic, Static> mapList(
                TypedProp.Multiple<Dynamic, DynamicProp> prop,
                String staticPropName, 
                Consumer<ListMapping<Static, DynamicProp>> block
        ) {
            return mapList(
                    prop.unwrap(),
                    staticPropName,
                    (Consumer<ListMapping<Static, ?>>)(Consumer<?>)block
            );
        }

        Builder<Dynamic, Static> mapList(
                ImmutableProp prop,
                String staticPropName,
                Consumer<ListMapping<Static, ?>> block
        );

        default Builder<Dynamic, Static> unmapStaticProps(String ... staticPropNames) {
            return unmapStaticProps(Arrays.asList(staticPropNames));
        }

        Builder<Dynamic, Static> unmapStaticProps(Collection<String> staticPropNames);

        Builder<Dynamic, Static> setDraftModifier(BiConsumer<Draft, Static> modifier);

        ImmutableConverter<Dynamic, Static> build();
    }

    interface Mapping<Static, DynamicProp> {

        Mapping<Static, DynamicProp> useIf(Predicate<Static> cond);

        Mapping<Static, DynamicProp> valueConverter(Function<?, DynamicProp> valueConverter);

        Mapping<Static, DynamicProp> nestedConverter(ImmutableConverter<DynamicProp, ?> valueConverter);

        Mapping<Static, DynamicProp> defaultValue(DynamicProp defaultValue);

        Mapping<Static, DynamicProp> defaultValue(Supplier<DynamicProp> defaultValueSupplier);
    }

    interface ListMapping<Static, DynamicProp> {

        ListMapping<Static, DynamicProp> useIf(Predicate<Static> cond);

        ListMapping<Static, DynamicProp> elementConverter(Function<?, DynamicProp> elementConverter);

        ListMapping<Static, DynamicProp> nestedConverter(ImmutableConverter<DynamicProp, ?> elementConverter);

        ListMapping<Static, DynamicProp> defaultElement(DynamicProp defaultElement);

        ListMapping<Static, DynamicProp> defaultElement(Supplier<DynamicProp> defaultValueSupplier);
    }
}
