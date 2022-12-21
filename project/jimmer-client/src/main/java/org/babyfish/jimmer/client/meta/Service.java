package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Service extends Node {

    Class<?> getJavaType();

    String getUri();

    Operation.HttpMethod getDefaultMethod();

    List<Operation> getOperations();

    @Nullable
    Document getDocument();
}
