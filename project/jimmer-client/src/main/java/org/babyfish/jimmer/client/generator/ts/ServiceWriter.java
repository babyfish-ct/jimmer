package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.generator.ts.simple.ExecutorWriter;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ServiceWriter extends TsCodeWriter {

    private final Service service;

    public ServiceWriter(TsContext ctx, Service service) {
        super(ctx, ctx.getFile(service));
        this.service = service;
    }

    @Override
    protected void write() {

        importFile(ExecutorWriter.FILE);

        document(service.getDocument());

        code("export class ").code(getFile().getName()).code(' ');
        scope(ScopeType.OBJECT, "", true, () -> {

            code("\nconstructor(private executor: Executor) {}\n");

            for (Operation operation : service.getOperations()) {
                write(operation);
            }
        });

        if (!getContext().isAnonymous()) {
            code('\n');
            code("\nexport type ").code(getFile().getName()).code("Options = ");
            scope(ScopeType.OBJECT, ",", true, () -> {
                for (Operation operation : service.getOperations()) {
                    separator();
                    code('\'').code(getContext().getOperationName(operation)).code("': ");
                    optionsBody(operation);
                }
            });
        }
    }

    private void write(Operation operation) {
        code('\n');
        document(operation.getDocument());
        code("async ").code(getContext().getOperationName(operation))
                .scope(ScopeType.ARGUMENTS, "", false, () -> {
                    if (!operation.getParameters().isEmpty()) {
                        code("options");
                        code(": ");
                        if (getContext().isAnonymous()) {
                            optionsBody(operation);
                        } else {
                            optionsName(operation);
                        }
                    }
                })
                .code(": Promise")
                .scope(
                        ScopeType.GENERIC,
                        ", ",
                        !(NullableType.unwrap(operation.getType()) instanceof SimpleType),
                        () -> {
                            typeRef(operation.getType());
                        }
                )
                .code(" ")
                .scope(ScopeType.OBJECT, "", true, () -> {
                    impl(operation);
                })
                .code('\n');
    }

    private void write(Parameter parameter) {
        code("readonly ")
                .code(parameter.getName())
                .codeIf(parameter.getType() instanceof NullableType, '?')
                .code(": ")
                .typeRef(NullableType.unwrap(parameter.getType()));
    }

    private void optionsName(Operation operation) {
        code(getFile().getName())
                .code("Options['")
                .code(getContext().getOperationName(operation))
                .code("']");
    }

    private void optionsBody(Operation operation) {
        scope(
                ScopeType.OBJECT,
                ", ",
                operation.getParameters().size() > 2,
                () -> {
                    for (Parameter parameter : operation.getParameters()) {
                        separator();
                        if (parameter.getDocument() != null) {
                            code('\n');
                            document(parameter.getDocument());
                        }
                        write(parameter);
                    }
                }
        );
    }

    private void impl(Operation operation) {
        List<UriPart> parts = UriPart.parts(operation.getUri());
        if (parts.get(0).variable) {
            Parameter parameter = pathVariableParameter(operation, parts.get(0).text);
            code("let _uri = encodeURIComponent(options.")
                    .code(parameter.getName())
                    .codeIf(parameter.getType() instanceof ArrayType, ".join(',')")
                    .code(");\n");
        } else {
            code("let _uri = '").code(parts.get(0).text).code("';\n");
        }
        for (int i = 1; i < parts.size(); i++) {
            if (parts.get(i).variable) {
                Parameter parameter = pathVariableParameter(operation, parts.get(i).text);
                code("_uri += encodeURIComponent(options.")
                        .code(parameter.getName())
                        .codeIf(parameter.getType() instanceof ArrayType, ".join(',')")
                        .code(");\n");
            } else {
                code("_uri += '").code(parts.get(i).text).code("';\n");
            }
        }

        Map<String, PathBuilder> pathBuilderMap = new LinkedHashMap<>();
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.getPathVariable() == null && parameter.getRequestParam() == null && !parameter.isRequestBody()) {
                PathBuilder builder = new PathBuilder();
                builder.dot().append(parameter.getName());
                Type type = parameter.getType();
                if (type instanceof NullableType) {
                    builder.nullable();
                    type = ((NullableType)type).getTargetType();
                }
                if (type instanceof ArrayType) {
                    builder.dot().append("join(',')");
                    pathBuilderMap.put(parameter.getName(), builder);
                } else if (type instanceof SimpleType) {
                    pathBuilderMap.put(parameter.getName(), builder);
                } else if (type instanceof ObjectType) {
                    for (Property prop : ((ObjectType)type).getProperties().values()) {
                        PathBuilder newBuilder = new PathBuilder(builder);
                        newBuilder.dot().append(prop.getName());
                        Type newType = prop.getType();
                        if (newType instanceof NullableType) {
                            newBuilder.nullable();
                            newType = ((NullableType)newType).getTargetType();
                        }
                        if (newType instanceof ArrayType) {
                            newBuilder.dot().append("join(',')");
                            pathBuilderMap.put(prop.getName(), newBuilder);
                        } else if (newType instanceof SimpleType) {
                            pathBuilderMap.put(prop.getName(), newBuilder);
                        }
                    }
                }
            }
        }
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.getRequestParam() != null) {
                PathBuilder builder = new PathBuilder();
                builder.dot().append(parameter.getName());
                Type type = parameter.getType();
                if (type instanceof NullableType) {
                    builder.nullable();
                    type = ((NullableType)type).getTargetType();
                }
                if (type instanceof ArrayType) {
                    builder.dot().append("join(',')");
                    pathBuilderMap.put(parameter.getName(), builder);
                } else if (type instanceof SimpleType) {
                    pathBuilderMap.put(parameter.getName(), builder);
                }
            }
        }

        if (!pathBuilderMap.isEmpty()) {
            code("let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n");
            code("let _value: any = undefined;\n");
            for (Map.Entry<String, PathBuilder> e : pathBuilderMap.entrySet()) {
                PathBuilder builder = e.getValue();
                code("_value = options").code(builder.toString()).code(";\n");
                code("if (_value !== undefined && _value !== null) ");
                scope(ScopeType.OBJECT, "", true, () -> {
                    code("_uri += _separator\n");
                    code("_uri += '").code(e.getKey() + "=").code("'\n");
                    code("_uri += encodeURIComponent(_value);\n");
                    code("_separator = '&';\n");
                });
                code("\n");
            }
        }
        code("return (await this.executor({uri: _uri, method: '")
                .code(operation.getHttpMethod().name())
                .code("'");
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.isRequestBody()) {
                code(", body: options.").code(parameter.getName());
            }
        }
        code("})) as ").typeRef(operation.getType());
    }

    private static Parameter pathVariableParameter(Operation operation, String pathVariable) {
        for (Parameter parameter : operation.getParameters()) {
            if (pathVariable.equals(parameter.getName())) {
                return parameter;
            }
        }
        throw new IllegalDocMetaException(
                "Illegal operation \"" +
                        operation.getRawMethod() +
                        "\", the path variable {" +
                        pathVariable +
                        "} cannot be resolved by any parameter"
        );
    }

    @Override
    protected boolean rawImmutableAsDynamic() {
        return true;
    }

    private static class UriPart {

        private final static Pattern SLASH_PATTERN = Pattern.compile("\\{[^\\}]+\\}");

        final String text;

        final boolean variable;

        private UriPart(String text, boolean variable) {
            this.text = text;
            this.variable = variable;
        }

        public static List<UriPart> parts(String uri) {
            if (!uri.startsWith("/")) {
                uri = '/' + uri;
            }
            List<UriPart> uriParts = new ArrayList<>();
            Matcher matcher = SLASH_PATTERN.matcher(uri);
            int pos = 0;
            while (matcher.find()) {
                if (matcher.start() > pos) {
                    uriParts.add(new UriPart(uri.substring(pos, matcher.start()), false));
                }
                uriParts.add(new UriPart(uri.substring(matcher.start() + 1, matcher.end() - 1), true));
                pos = matcher.end();
            }
            if (pos < uri.length()) {
                uriParts.add(new UriPart(uri.substring(pos, uri.length()), false));
            }
            return uriParts;
        }
    }

    private static class PathBuilder {

        private final StringBuilder builder;

        private boolean nullable;

        PathBuilder() {
            this.builder = new StringBuilder();
        }

        PathBuilder(PathBuilder base) {
            this.builder = new StringBuilder(base.builder);
            this.nullable = base.nullable;
        }

        public PathBuilder nullable() {
            this.nullable = true;
            return this;
        }

        public PathBuilder dot() {
            if (nullable) {
                builder.append("?.");
            } else {
                builder.append('.');
            }
            return this;
        }

        public PathBuilder append(String text) {
            int size = text.length();
            for (int i = 0; i < size; i++) {
                char c = text.charAt(i);
                if (nullable && c == '.') {
                    builder.append("?.");
                } else {
                    builder.append(c);
                }
            }
            return this;
        }

        public String toString() {
            return builder.toString();
        }
    }
}
