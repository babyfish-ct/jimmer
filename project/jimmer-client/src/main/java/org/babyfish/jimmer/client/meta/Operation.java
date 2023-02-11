package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public interface Operation extends Node {

    Service getDeclaringService();

    String getName();

    String getUri();

    HttpMethod getHttpMethod();

    Method getRawMethod();

    List<Parameter> getParameters();

    Type getType();

    List<EnumBasedError> getErrors();

    @Nullable
    Document getDocument();

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }
}
