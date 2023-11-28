package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface TypeRef {

    TypeName getTypeName();

    boolean isNullable();

    List<TypeRef> getArguments();

    @Nullable
    String getFetchBy();

    @Nullable
    String getFetcherOwner();
}
