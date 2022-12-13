package org.babyfish.jimmer.client.meta;

public interface Parameter extends Node {

    Operation getDeclaringOperation();

    String getName();

    Type getType();

    int getOriginalIndex();

    String getRequestParam();

    String getPathVariable();

    boolean isRequestBody();
}
