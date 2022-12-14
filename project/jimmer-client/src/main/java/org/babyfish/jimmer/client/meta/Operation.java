package org.babyfish.jimmer.client.meta;

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

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }
}
