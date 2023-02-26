package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

class ParameterImpl implements Parameter {

    private static final Pattern UNNAMED_PATTERN = Pattern.compile("arg\\d+");

    private final Operation declaringOperation;

    private final java.lang.reflect.Parameter rawParameter;

    private final int originalIndex;

    private final String requestParam;

    private final String pathVariable;

    private final boolean body;

    private final Type type;

    private final Document document;

    private final String name;

    ParameterImpl(
            Operation declaringOperation,
            java.lang.reflect.Parameter rawParameter,
            String parameterName,
            int originalIndex,
            String requestParam,
            String pathVariable,
            boolean body,
            Type type
    ) {
        this.declaringOperation = declaringOperation;
        this.rawParameter = rawParameter;
        this.originalIndex = originalIndex;
        this.requestParam = requestParam;
        this.pathVariable = pathVariable;
        this.body = body;
        this.type = type;
        this.document = DocumentImpl.of(rawParameter);
        if (this.requestParam != null) {
            name = this.requestParam;
        } else if (this.pathVariable != null) {
            name = this.pathVariable;
        } else if (this.body) {
            name = "body";
        } else if (parameterName != null) {
            name = parameterName;
        } else {
            name = rawParameter.getName();
        }
    }

    @Override
    public Operation getDeclaringOperation() {
        return declaringOperation;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int getOriginalIndex() {
        return originalIndex;
    }

    @Override
    public String getRequestParam() {
        return requestParam;
    }

    @Override
    public String getPathVariable() {
        return pathVariable;
    }

    @Override
    public boolean isRequestBody() {
        return body;
    }

    @Nullable
    @Override
    public Document getDocument() {
        return document;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitParameter(this);
        type.accept(visitor);
    }

    @Override
    public String toString() {
        return getName() + ": " + getType();
    }

    @Nullable
    static Parameter create(
            Context ctx,
            Operation declaringOperation,
            java.lang.reflect.Parameter rawParameter,
            String parameterName,
            int index
    ) {
        Metadata.ParameterParser parameterParser = ctx.getParameterParser();
        JetBrainsMetadata jetBrainsMetadata = ctx.getJetBrainsMetadata(declaringOperation.getRawMethod().getDeclaringClass());
        boolean isNullable = jetBrainsMetadata.isNullable(declaringOperation.getRawMethod(), index);
        if (parameterName == null) {
            parameterName = rawParameter.getName();
        }

        Tuple2<String, Boolean> tuple = parameterParser.requestParamNameAndNullable(rawParameter);
        String requestParam = tuple != null ? tuple.get_1() : null;
        if (requestParam != null) {
            if (requestParam.isEmpty()) {
                requestParam = parameterName;
            }
            Type type = ctx
                    .locate(new ParameterLocation(declaringOperation, index, rawParameter.getName()))
                    .parseType(rawParameter.getAnnotatedType());
            if (isNullable) {
                type = NullableTypeImpl.of(type);
            }
            if (tuple.get_2()) {
                type = NullableTypeImpl.of(type);
            }
            return new ParameterImpl(declaringOperation, rawParameter, parameterName, index, requestParam, null, false, type);
        }

        String pathVariable = parameterParser.pathVariableName(rawParameter);
        if (pathVariable != null) {
            if (pathVariable.isEmpty()) {
                pathVariable = parameterName;
            }
            Type type = ctx
                    .locate(new ParameterLocation(declaringOperation, index, rawParameter.getName()))
                    .parseType(rawParameter.getAnnotatedType());
            // Need not `Utils.wrap` because path variable should be considered as non-null in client side
            return new ParameterImpl(declaringOperation, rawParameter, parameterName, index, null, pathVariable, false, type);
        }

        if (parameterParser.isRequestBody(rawParameter)) {
            Type type = ctx
                    .locate(new ParameterLocation(declaringOperation, index, rawParameter.getName()))
                    .parseType(rawParameter.getAnnotatedType());
            if (isNullable) {
                type = NullableTypeImpl.of(type);
            }
            return new ParameterImpl(declaringOperation, rawParameter, parameterName, index, null, null, true, type);
        }

        if (!parameterParser.shouldBeIgnored(rawParameter)) {
            Type type = ctx
                    .locate(new ParameterLocation(declaringOperation, index, rawParameter.getName()))
                    .parseType(rawParameter.getAnnotatedType());
            if (isNullable) {
                type = NullableTypeImpl.of(type);
            }
            return new ParameterImpl(declaringOperation, rawParameter, parameterName, index, null, null, false, type);
        }
        return null;
    }

    private static class ParameterLocation implements Location {

        private final Operation declaringOperation;

        private final int index;

        private final String name;

        private ParameterLocation(Operation declaringOperation, int index, String name) {
            this.declaringOperation = declaringOperation;
            this.index = index;
            this.name = name;
        }

        @Override
        public boolean isQueryResult() {
            return false;
        }

        @Override
        public Class<?> getDeclaringType() {
            return declaringOperation.getDeclaringService().getJavaType();
        }

        @Override
        public String toString() {
            return "The parameter(index = " +
                    index +
                    ", name = " +
                    name +
                    ") of \"" +
                    declaringOperation.getRawMethod() +
                    "\"";
        }
    }
}
