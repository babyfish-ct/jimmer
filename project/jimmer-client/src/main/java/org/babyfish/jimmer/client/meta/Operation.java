package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public interface Operation extends Node {

    Service getDeclaringService();

    String getName();

    String getUri();

    HttpMethod getHttpMethod();

    Method getRawMethod();

    List<Parameter> getParameters();

    Type getType();

    @Nullable
    Document getDocument();

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }
}
