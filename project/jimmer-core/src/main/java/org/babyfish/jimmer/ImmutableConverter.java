package org.babyfish.jimmer;

import org.babyfish.jimmer.impl.converter.ImmutableConverterBuilderImpl;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

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

        default Builder<T, Static> map(TypedProp<T, ?> prop) {
            return mapIf(null, prop.unwrap(), prop.unwrap().getName(), null);
        }

        default Builder<T, Static> map(ImmutableProp prop) {
            return mapIf(null, prop, prop.getName(), null);
        }

        default Builder<T, Static> map(TypedProp<T, ?> prop, String staticPropName) {
            return mapIf(null, prop.unwrap(), staticPropName, null);
        }

        default Builder<T, Static> map(ImmutableProp prop, String staticPropName) {
            return mapIf(null, prop, staticPropName, null);
        }

        default Builder<T, Static> map(
                TypedProp<T, ?> prop,
                ValueConverter valueConverter
        ) {
            return mapIf(null, prop.unwrap(), prop.unwrap().getName(), valueConverter);
        }

        default Builder<T, Static> map(
                ImmutableProp prop,
                ValueConverter valueConverter
        ) {
            return mapIf(null, prop, prop.getName(), valueConverter);
        }

        default Builder<T, Static> map(
                TypedProp<T, ?> prop,
                String staticPropName,
                ValueConverter valueConverter
        ) {
            return mapIf(null, prop.unwrap(), staticPropName, valueConverter);
        }

        default Builder<T, Static> map(
                ImmutableProp prop,
                String staticPropName,
                ValueConverter valueConverter
        ) {
            return mapIf(null, prop, staticPropName, valueConverter);
        }

        default Builder<T, Static> mapList(
                TypedProp.Multiple<T, ?> prop,
                ValueConverter elementConverter
        ) {
            return mapListIf(null, prop.unwrap(), prop.unwrap().getName(), elementConverter);
        }

        default Builder<T, Static> mapList(
                ImmutableProp prop,
                ValueConverter elementConverter
        ) {
            return mapListIf(null, prop, prop.getName(), elementConverter);
        }

        default Builder<T, Static> mapList(
                TypedProp.Multiple<T, ?> prop,
                String staticPropName,
                ValueConverter elementConverter
        ) {
            return mapListIf(null, prop.unwrap(), staticPropName, elementConverter);
        }

        default Builder<T, Static> mapList(
                ImmutableProp prop,
                String staticPropName,
                ValueConverter elementConverter
        ) {
            return mapListIf(null, prop, staticPropName, elementConverter);
        }

        default Builder<T, Static> mapIf(Predicate<Static> cond, TypedProp<T, ?> prop) {
            return mapIf(cond, prop.unwrap(), prop.unwrap().getName(), null);
        }

        default Builder<T, Static> mapIf(Predicate<Static> cond, ImmutableProp prop) {
            return mapIf(cond, prop, prop.getName(), null);
        }

        default Builder<T, Static> mapIf(Predicate<Static> cond, TypedProp<T, ?> prop, String staticPropName) {
            return mapIf(cond, prop.unwrap(), staticPropName, null);
        }

        default Builder<T, Static> mapIf(Predicate<Static> cond, ImmutableProp prop, String staticPropName) {
            return mapIf(cond, prop, staticPropName, null);
        }

        default Builder<T, Static> mapIf(
                Predicate<Static> cond,
                TypedProp<T, ?> prop,
                ValueConverter valueConverter
        ) {
            return mapIf(cond, prop.unwrap(), prop.unwrap().getName(), valueConverter);
        }

        default Builder<T, Static> mapIf(
                Predicate<Static> cond,
                ImmutableProp prop,
                ValueConverter valueConverter
        ) {
            return mapIf(cond, prop, prop.getName(), valueConverter);
        }

        default Builder<T, Static> mapIf(
                Predicate<Static> cond,
                TypedProp<T, ?> prop,
                String staticPropName,
                ValueConverter valueConverter
        ) {
            return mapIf(cond, prop.unwrap(), staticPropName, valueConverter);
        }

        Builder<T, Static> mapIf(
                Predicate<Static> cond,
                ImmutableProp prop,
                String staticPropName,
                ValueConverter valueConverter
        );

        default Builder<T, Static> mapListIf(
                Predicate<Static> cond,
                TypedProp.Multiple<T, ?> prop,
                ValueConverter elementConverter
        ) {
            return mapListIf(cond, prop.unwrap(), prop.unwrap().getName(), elementConverter);
        }

        default Builder<T, Static> mapListIf(
                Predicate<Static> cond,
                ImmutableProp prop,
                ValueConverter elementConverter
        ) {
            return mapListIf(cond, prop, prop.getName(), elementConverter);
        }

        default Builder<T, Static> mapListIf(
                Predicate<Static> cond,
                TypedProp.Multiple<T, ?> prop,
                String staticPropName,
                ValueConverter elementConverter
        ) {
            return mapListIf(cond, prop.unwrap(), staticPropName, elementConverter);
        }

        Builder<T, Static> mapListIf(
                Predicate<Static> cond,
                ImmutableProp prop,
                String staticPropName,
                ValueConverter elementConverter
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
    
    @FunctionalInterface
    interface ValueConverter {
        
        Object convert(Object value);
        
        default Object defaultValue() {
            return null;
        }
    }
}
