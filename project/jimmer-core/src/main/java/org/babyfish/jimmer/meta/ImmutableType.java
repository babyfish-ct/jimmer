package org.babyfish.jimmer.meta;

import kotlin.reflect.KClass;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.impl.Metadata;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.runtime.DraftContext;

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
    
    Class<?> getJavaClass();

    boolean isKotlinClass();

    boolean isEntity();

    boolean isMappedSuperclass();

    boolean isEmbeddable();

    Annotation getImmutableAnnotation();

    boolean isAssignableFrom(ImmutableType type);

    ImmutableType getSuperType();

    BiFunction<DraftContext, Object, Draft> getDraftFactory();

    Map<String, ImmutableProp> getDeclaredProps();

    ImmutableProp getIdProp();

    ImmutableProp getVersionProp();

    Set<ImmutableProp> getKeyProps();

    String getTableName();

    Map<String, ImmutableProp> getProps();

    ImmutableProp getProp(String name);

    ImmutableProp getProp(int id);

    List<ImmutableProp> getPropChainByColumnName(String columnName);

    Map<String, ImmutableProp> getSelectableProps();

    Map<String, ImmutableProp> getSelectableReferenceProps();

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
