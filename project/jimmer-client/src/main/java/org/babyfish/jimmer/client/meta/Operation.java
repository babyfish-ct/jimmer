package org.babyfish.jimmer.client.meta;

import java.lang.reflect.Method;
import java.util.Map;

public interface Operation extends Node {

    Service getDeclaringService();

    String getName();

    HttpMethod getHttpMethod();

    Method getRawMethod();

    Map<String, Parameter> getParameters();

    Type getType();

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }
}
