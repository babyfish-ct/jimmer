package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;

import java.lang.reflect.Method;
import java.util.List;

public interface Operation {

    Service getDeclaringService();

    String getName();

    Doc getDoc();

    List<HttpMethod> getHttpMethods();

    String getUri();

    Method getJavaMethod();

    List<Parameter> getParameters();

    Type getReturnType();

    List<ObjectType> getExceptionTypes();

    enum HttpMethod {
        GET,
        HEAD,
        POST,
        PUT,
        PATCH,
        DELETE,
        OPTIONS,
        TRACE
    }
}
