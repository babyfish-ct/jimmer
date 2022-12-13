package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

class ParameterImpl implements Parameter {

    private final Operation declaringOperation;

    private final java.lang.reflect.Parameter rawParameter;

    private final int originalIndex;

    private final String requestParam;

    private final String pathVariable;

    private final Type type;

    ParameterImpl(
            Operation declaringOperation,
            java.lang.reflect.Parameter rawParameter,
            int originalIndex,
            String requestParam,
            String pathVariable,
            Type type) {
        this.declaringOperation = declaringOperation;
        this.rawParameter = rawParameter;
        this.originalIndex = originalIndex;
        this.requestParam = requestParam;
        this.pathVariable = pathVariable;
        this.type = type;
    }

    @Override
    public Operation getDeclaringOperation() {
        return declaringOperation;
    }

    @Override
    public String getName() {
        return rawParameter.getName();
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
        return requestParam == null && pathVariable == null;
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
            int index
    ) {
        Metadata.ParameterParser parameterParser = ctx.getParameterParser();
        String requestParam = parameterParser.requestParamName(rawParameter);

        if (requestParam != null) {
            if (requestParam.isEmpty()) {
                requestParam = rawParameter.getName();
            }
            Type type = ctx
                    .locate(new ParameterLocation(declaringOperation, index, rawParameter.getName()))
                    .parseType(rawParameter.getAnnotatedType());
            return new ParameterImpl(declaringOperation, rawParameter, index, requestParam, null, type);
        }

        String pathVariable = parameterParser.pathVariableName(rawParameter);
        if (pathVariable != null) {
            if (pathVariable.isEmpty()) {
                pathVariable = rawParameter.getName();
            }
            Type type = ctx
                    .locate(new ParameterLocation(declaringOperation, index, rawParameter.getName()))
                    .parseType(rawParameter.getAnnotatedType());
            return new ParameterImpl(declaringOperation, rawParameter, index, null, pathVariable, type);
        }

        if (parameterParser.isRequestBody(rawParameter)) {
            Type type = ctx
                    .locate(new ParameterLocation(declaringOperation, index, rawParameter.getName()))
                    .parseType(rawParameter.getAnnotatedType());
            return new ParameterImpl(declaringOperation, rawParameter, index, null, null, type);
        }

        return null;
    }

    private static Type parameterType(
            Context ctx,
            Operation declaringOperation,
            java.lang.reflect.Parameter rawParameter,
            int index
    ) {
        Type type = ctx
                .locate(new ParameterLocation(declaringOperation, index, rawParameter.getName()))
                .parseType(rawParameter.getAnnotatedType());
        if (ctx.getParameterParser().isOptional(rawParameter)) {
            return NullableTypeImpl.of(type);
        }
        return Utils.wrap(type, rawParameter);
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
            return declaringOperation.getDeclaringService().getRawType();
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
