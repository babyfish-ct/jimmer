package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.IgnoreApi;
import org.babyfish.jimmer.client.IgnoreParam;
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

    private final Collection<EnumBasedError> errors;

    private final Document document;

    List<Parameter> parameters;

    OperationImpl(
            Service declaringService,
            Method rawMethod,
            String uri,
            HttpMethod method,
            Type type,
            Collection<EnumBasedError> errors
    ) {
        this.declaringService = declaringService;
        this.rawMethod = rawMethod;
        this.uri = uri;
        this.httpMethod = method;
        this.type = type;
        this.errors = errors;
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

    @Override
    public Collection<EnumBasedError> getErrors() {
        return errors;
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
        for (EnumBasedError error : errors) {
            for (EnumBasedError.Field field : error.getFields().values()) {
                field.getType().accept(visitor);
            }
        }
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
    static Operation create(
            Context ctx,
            Service declaringService,
            java.lang.reflect.Method rawMethod
    ) {
        if (rawMethod.isAnnotationPresent(IgnoreApi.class)) {
            return null;
        }
        Tuple2<String, HttpMethod> http = ctx.getOperationParser().http(rawMethod);
        if (http == null) {
            return null;
        }
        String uri = http.get_1();
        if (uri == null || uri.isEmpty()) {
            uri = "/";
        }
        String parentUri = declaringService.getUri();
        if (parentUri != null) {
            if (parentUri.endsWith("/") && uri.startsWith("/")) {
                uri = parentUri + uri.substring(1);
            } else if (!parentUri.endsWith("/") && !uri.startsWith("/")) {
                uri = parentUri + '/' + uri;
            } else {
                uri = parentUri + uri;
            }
        }
        HttpMethod httpMethod = http.get_2();
        if (httpMethod == null) {
            httpMethod = declaringService.getDefaultMethod();
        }
        Context subContext = ctx.locate(new OperationLocation(rawMethod, httpMethod));
        JetBrainsMetadata jetBrainsMetadata = ctx.getJetBrainsMetadata(rawMethod.getDeclaringClass());

        Type type =
                jetBrainsMetadata.isKotlinClass() ?
                        subContext.parseKotlinType(
                                ctx.getOperationParser().kotlinType(
                                        jetBrainsMetadata.toKFunction(rawMethod)
                                )
                        ) :
                        subContext.parseType(
                                ctx.getOperationParser().javaType(rawMethod)
                        );
        if (jetBrainsMetadata.isNullable(rawMethod)) {
            type = NullableTypeImpl.of(type);
        }
        OperationImpl operation = new OperationImpl(
                declaringService,
                rawMethod,
                uri,
                httpMethod,
                type,
                new Throws(ctx).getErrors(rawMethod)
        );

        if (declaringService.getJavaType().isAnnotationPresent(IgnoreParam.class) || rawMethod.isAnnotationPresent(IgnoreParam.class)) {
            operation.parameters = Collections.emptyList();
            return operation;
        }

        int index = 0;
        String[] parameterNames = ctx.getOperationParser().getParameterNames(rawMethod);
        List<Parameter> list = new ArrayList<>();
        java.lang.reflect.Parameter[] rawParameters = rawMethod.getParameters();
        for (int i = 0; i < rawParameters.length; i++) {

            if (rawParameters[i].isAnnotationPresent(IgnoreParam.class)) {
                continue;
            }

            Parameter parameter = ParameterImpl.create(
                    ctx,
                    operation,
                    rawParameters[i],
                    parameterNames != null && parameterNames.length != 0 ?
                            parameterNames[i] :
                            null,
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
