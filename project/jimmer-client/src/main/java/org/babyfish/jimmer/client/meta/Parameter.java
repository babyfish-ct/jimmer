package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

public interface Parameter extends Node {

    Operation getDeclaringOperation();

    String getName();

    Type getType();

    int getOriginalIndex();

    String getRequestParam();

    String getPathVariable();

    boolean isRequestBody();

    @Nullable
    Document getDocument();
}
