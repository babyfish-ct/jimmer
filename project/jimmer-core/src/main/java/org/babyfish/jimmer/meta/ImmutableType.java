package org.babyfish.jimmer.meta;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.meta.impl.Metadata;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.SequenceIdGenerator;
import org.babyfish.jimmer.runtime.DraftContext;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
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
    
    Class<?> getJavaClass();

    ImmutableType getSuperType();

    BiFunction<DraftContext, Object, Draft> getDraftFactory();

    Map<String, ImmutableProp> getDeclaredProps();

    ImmutableProp getIdProp();

    ImmutableProp getVersionProp();

    Set<ImmutableProp> getKeyProps();

    String getTableName();

    Map<String, ImmutableProp> getProps();

    ImmutableProp getProp(String name);

    Map<String, ImmutableProp> getSelectableProps();

    IdGenerator getIdGenerator();

    interface Builder {

        Builder id(String name, Class<?> elementType);

        Builder key(String name, Class<?> elementType);

        Builder keyReference(
                String name,
                Class<?> elementType,
                boolean nullable
        );

        Builder version(String name);

        Builder add(
                String name,
                ImmutablePropCategory category,
                Class<?> elementType,
                boolean nullable
        );

        Builder add(
                String name,
                ImmutablePropCategory category,
                Class<?> elementType,
                boolean nullable,
                Class<? extends Annotation> associationType
        );

        ImmutableType build();
    }
}
