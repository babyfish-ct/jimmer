package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

class OperationImpl implements Operation {

    private final Service declaringService;

    private final java.lang.reflect.Method rawMethod;

    private final String uri;

    private final HttpMethod httpMethod;

    private final Type type;

    private final Document document;

    List<Parameter> parameters;

    OperationImpl(
            Service declaringService,
            Method rawMethod,
            String uri,
            HttpMethod method,
            Type type
    ) {
        this.declaringService = declaringService;
        this.rawMethod = rawMethod;
        this.uri = uri;
        this.httpMethod = method;
        this.type = type;
        this.document = DocumentImpl.of(rawMethod);
    }

    @Override
    public Service getDeclaringService() {
        return declaringService;
    }

    @Override
    public String getName() {
        return rawMethod.getName();
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    @Override
    public Method getRawMethod() {
        return rawMethod;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Nullable
    @Override
    public Document getDocument() {
        return document;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitingOperation(this);
        for (Parameter parameter : parameters) {
            parameter.accept(visitor);
        }
        type.accept(visitor);
        visitor.visitedOperation(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName()).append('(');
        boolean addComma = false;
        for (Parameter parameter : parameters) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(parameter);
        }
        builder.append("): ").append(getType());
        return builder.toString();
    }

    @Nullable
    static Operation create(Context ctx, Service declaringService, java.lang.reflect.Method rawMethod) {
        Tuple2<String, HttpMethod> http = ctx.getOperationParser().http(rawMethod);
        if (http == null || http.get_1() == null) {
            return null;
        }
        HttpMethod httpMethod = http.get_2();
        if (httpMethod == null) {
            httpMethod = HttpMethod.GET;
        }
        Context subContext = ctx.locate(new OperationLocation(rawMethod, httpMethod));
        JetBrainsMetadata jetBrainsMetadata = ctx.getJetBrainsMetadata(rawMethod.getDeclaringClass());
        Type type =
                jetBrainsMetadata.isKotlinClass() ?
                        subContext.parseKotlinType(jetBrainsMetadata.toKFunction(rawMethod).getReturnType()) :
                        subContext.parseType(rawMethod.getAnnotatedReturnType());
        if (jetBrainsMetadata.isNullable(rawMethod)) {
            type = NullableTypeImpl.of(type);
        }
        OperationImpl operation = new OperationImpl(declaringService, rawMethod, http.get_1(), httpMethod, type);
        int index = 0;
        List<Parameter> list = new ArrayList<>();
        for (java.lang.reflect.Parameter rawParameter : rawMethod.getParameters()) {
            Parameter parameter = ParameterImpl.create(
                    ctx,
                    operation,
                    rawParameter,
                    index++
            );
            if (parameter != null) {
                list.add(parameter);
            }
        }
        if (list.size() > 1) {
            long requestBodyCount = list.stream().filter(Parameter::isRequestBody).count();
            if (requestBodyCount > 1) {
                throw new IllegalDocMetaException(
                        "Illegal method \"" +
                                rawMethod +
                                "\", can only contain one request body parameter"
                );
            }
        }
        operation.parameters = Collections.unmodifiableList(list);
        Set<String> names = new HashSet<>();
        for (Parameter parameter : list) {
            if (!names.add(parameter.getName())) {
                throw new IllegalDocMetaException(
                        "Illegal method \"" +
                                rawMethod +
                                "\", duplicated parameter name \"" +
                                parameter.getName() +
                                "\"(web parameter name, not java parameter name)"
                );
            }
        }
        return operation;
    }

    private static class OperationLocation implements Location {

        private final Method rawMethod;

        private final HttpMethod httpMethod;

        private OperationLocation(Method rawMethod, HttpMethod httpMethod) {
            this.rawMethod = rawMethod;
            this.httpMethod = httpMethod;
        }

        @Override
        public boolean isQueryResult() {
            return httpMethod == HttpMethod.GET;
        }

        @Override
        public Class<?> getDeclaringType() {
            return rawMethod.getDeclaringClass();
        }

        @Override
        public String toString() {
            return "return type of " + rawMethod;
        }
    }
}
