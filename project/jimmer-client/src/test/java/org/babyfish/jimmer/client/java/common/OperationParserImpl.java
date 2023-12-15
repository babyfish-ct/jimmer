package org.babyfish.jimmer.client.java.common;

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
        return null;
    }
}
