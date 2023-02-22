package org.babyfish.jimmer.meta;

import kotlin.reflect.KClass;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.impl.Metadata;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.runtime.DraftContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.BiFunction;

public interface ImmutableType {

    static ImmutableType get(Class<?> javaClass) {
        return Metadata.get(javaClass);
    }

    static ImmutableType tryGet(Class<?> javaClass) {
        return Metadata.tryGet(javaClass);
    }

    static Builder newBuilder(
            Class<?> javaClass,
            ImmutableType superType,
            BiFunction<DraftContext, Object, Draft> draftFactory
    ) {
        return Metadata.newTypeBuilder(javaClass, superType, draftFactory);
    }

    static Builder newBuilder(
            KClass<?> kotlinClass,
            ImmutableType superType,
            BiFunction<DraftContext, Object, Draft> draftFactory
    ) {
        return Metadata.newTypeBuilder(kotlinClass, superType, draftFactory);
    }

    @NotNull
    Class<?> getJavaClass();

    boolean isKotlinClass();

    boolean isEntity();

    boolean isMappedSuperclass();

    boolean isEmbeddable();

    @NotNull
    Annotation getImmutableAnnotation();

    boolean isAssignableFrom(ImmutableType type);

    @Nullable
    ImmutableType getSuperType();

    @NotNull
    BiFunction<DraftContext, Object, Draft> getDraftFactory();

    @NotNull
    Map<String, ImmutableProp> getDeclaredProps();

    /**
     * @return The id property declared in this type of super types.
     * <ul>
     *     <li>If the current type is decorated by {@link org.babyfish.jimmer.sql.Entity}, returns non-null value</li>
     *     <li>If the current type is decorated by {@link org.babyfish.jimmer.sql.MappedSuperclass},
     *     find id property in current type of super types, if nothing can be found, return null</li>
     *     <li>Otherwise, always returns null</li>
     * </ul>
     */
    ImmutableProp getIdProp();

    /**
     * @return The version property declared in this type of super types.
     * <ul>
     *     <li>If the current type is decorated by {@link org.babyfish.jimmer.sql.Entity}, returns non-null value</li>
     *     <li>If the current type is decorated by {@link org.babyfish.jimmer.sql.MappedSuperclass},
     *     find version property in current type of super types, if nothing can be found, return null</li>
     *     <li>Otherwise, always returns null</li>
     * </ul>
     */
    @Nullable
    ImmutableProp getVersionProp();

    /**
     * Get the logical deleted property declared in this type, exclude super types
     * @return The logical deleted property, may be null.
     */
    @Nullable
    LogicalDeletedInfo getDeclaredLogicalDeletedInfo();

    @NotNull
    Set<ImmutableProp> getKeyProps();

    @Nullable
    String getTableName();

    @NotNull
    Map<String, ImmutableProp> getProps();

    @NotNull
    ImmutableProp getProp(String name);

    @NotNull
    ImmutableProp getProp(int id);

    @NotNull
    List<ImmutableProp> getPropChainByColumnName(String columnName);

    @NotNull
    Map<String, ImmutableProp> getSelectableProps();

    @NotNull
    Map<String, ImmutableProp> getSelectableReferenceProps();

    @Nullable
    IdGenerator getIdGenerator();

    interface Builder {

        Builder id(int id, String name, Class<?> elementType);

        Builder key(int id, String name, Class<?> elementType);

        Builder keyReference(
                int id,
                String name,
                Class<?> elementType,
                boolean nullable
        );

        Builder version(int id, String name);

        Builder logicalDeleted(
                int id,
                String name,
                Class<?> elementType,
                boolean nullable
        );

        Builder add(
                int id,
                String name,
                ImmutablePropCategory category,
                Class<?> elementType,
                boolean nullable
        );

        Builder add(
                int id,
                String name,
                Class<? extends Annotation> associationType,
                Class<?> elementType,
                boolean nullable
        );

        ImmutableType build();
    }
}
