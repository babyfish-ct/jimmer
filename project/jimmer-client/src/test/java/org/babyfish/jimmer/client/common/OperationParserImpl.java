package org.babyfish.jimmer.client.common;

import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.Operation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class OperationParserImpl implements Metadata.OperationParser {

    @Override
    public String uri(AnnotatedElement element) {
        return null;
    }

    @Override
    public Operation.HttpMethod http(Method method) {
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.method().length != 0) {
            return requestMapping.method()[0];
        }
        if (method.getAnnotation(PutMapping.class) != null) {
            return Operation.HttpMethod.PUT;
        }
        if (method.getAnnotation(DeleteMapping.class) != null) {
            return Operation.HttpMethod.DELETE;
        }
        return Operation.HttpMethod.GET;
    }
}
