package org.babyfish.jimmer.client.common;

import org.babyfish.jimmer.client.java.model.StreamingResponseBody;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.Operation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class OperationParserImpl implements Metadata.OperationParser {

    @Override
    public String uri(AnnotatedElement element) {
        GetMapping getMapping = element.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return getMapping.value();
        }
        PostMapping postMapping = element.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            return postMapping.value();
        }
        PutMapping putMapping = element.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            return putMapping.value();
        }
        DeleteMapping deleteMapping = element.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return deleteMapping.value();
        }
        PatchMapping patchMapping = element.getAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            return patchMapping.value();
        }
        return null;
    }

    @Override
    public Operation.HttpMethod[] http(Method method) {
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.method().length != 0) {
            return requestMapping.method();
        }
        if (method.getAnnotation(PostMapping.class) != null) {
            return new Operation.HttpMethod[] { Operation.HttpMethod.POST };
        }
        if (method.getAnnotation(PutMapping.class) != null) {
            return new Operation.HttpMethod[] { Operation.HttpMethod.PUT };
        }
        if (method.getAnnotation(DeleteMapping.class) != null) {
            return new Operation.HttpMethod[] { Operation.HttpMethod.DELETE };
        }
        if (method.getAnnotation(PatchMapping.class) != null) {
            return new Operation.HttpMethod[] { Operation.HttpMethod.PATCH };
        }
        return new Operation.HttpMethod[] { Operation.HttpMethod.GET };
    }

    @Override
    public boolean isStream(Method method) {
        return StreamingResponseBody.class == method.getReturnType();
    }
}
