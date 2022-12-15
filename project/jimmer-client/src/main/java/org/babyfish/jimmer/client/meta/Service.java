package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Service extends Node {

    Class<?> getJavaType();

    List<Operation> getOperations();

    @Nullable
    Document getDocument();
}
