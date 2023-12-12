package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;

import java.lang.reflect.Method;
import java.util.List;

public interface Operation {

    String getName();

    Doc getDoc();

    HttpMethod getHttpMethod();

    String getUri();

    Method getJavaMethod();

    List<Parameter> getParameters();

    Type getReturnType();

    List<ObjectType> getExceptionTypes();

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }
}
