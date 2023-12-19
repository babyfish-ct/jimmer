package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface ObjectType extends Type {

    Class<?> getJavaType();

    @Nullable
    ImmutableType getImmutableType();

    Kind getKind();

    List<String> getSimpleNames();

    @Nullable
    FetchByInfo getFetchByInfo();

    List<Type> getArguments();

    @Nullable
    Doc getDoc();

    @Nullable
    TypeDefinition.Error getError();

    Map<String, Property> getProperties();

    boolean isRecursiveFetchedType();

    ObjectType unwrap();

    enum Kind {
        FETCHED,
        DYNAMIC,
        EMBEDDABLE,
        STATIC,
        ERROR
    }
}
