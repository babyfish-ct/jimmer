package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.runtime.impl.IllegalApiException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OperationRender implements Render {

    private final String name;

    private final Operation operation;

    public OperationRender(String name, Operation operation) {
        this.name = name;
        this.operation = operation;
    }

    @Override
    public void render(SourceWriter writer) {
        writer
                .code('\n')
                .doc(operation.getDoc(), SourceWriter.DocPart.PARAM, SourceWriter.DocPart.RETURN)
                .code("async ")
                .code(name);
        if (!operation.getParameters().isEmpty()) {
            writer.scope(SourceWriter.ScopeType.ARGUMENTS, ", ", false, () -> {
                writer.code("options: ").code(writer.getSource().getRoot().getName()).code("Options['").code(name).code("']");
            });
        } else {
            writer.code("()");
        }
        writer.code(": Promise");
        if (operation.getReturnType() == null) {
            writer.code("<void>");
        } else {
            writer.scope(SourceWriter.ScopeType.GENERIC, ", ", true, () -> {
                writer.typeRef(operation.getReturnType());
            });
        }
        writer.code(' ').scope(SourceWriter.ScopeType.OBJECT, "", true, () -> {
            renderImpl(writer);
        });
        writer.code('\n');
    }

    private void renderImpl(SourceWriter writer) {
        List<UriPart> parts = UriPart.parts(operation.getUri());
        if (parts.get(0).variable) {
            Parameter parameter = pathVariableParameter(operation, parts.get(0).text);
            writer.code("let _uri = encodeURIComponent(options.")
                    .code(parameter.getName())
                    .codeIf(parameter.getType() instanceof ListType, ".join(',')")
                    .code(");\n");
        } else {
            writer.code("let _uri = '").code(parts.get(0).text).code("';\n");
        }

        for (int i = 1; i < parts.size(); i++) {
            if (parts.get(i).variable) {
                Parameter parameter = pathVariableParameter(operation, parts.get(i).text);
                writer.code("_uri += encodeURIComponent(options.")
                        .code(parameter.getName())
                        .codeIf(parameter.getType() instanceof ListType, ".join(',')")
                        .code(");\n");
            } else {
                writer.code("_uri += '").code(parts.get(i).text).code("';\n");
            }
        }

        Map<String, PathBuilder> pathBuilderMap = new LinkedHashMap<>();
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.getPathVariable() == null &&
                    parameter.getRequestParam() == null &&
                    parameter.getRequestHeader() == null &&
                    !parameter.isRequestBody()) {
                PathBuilder builder = new PathBuilder();
                builder.dot().append(parameter.getName());
                Type type = parameter.getType();
                if (type instanceof NullableType) {
                    builder.nullable();
                    type = ((NullableType) type).getTargetType();
                }
                if (type instanceof ListType) {
                    builder.dot().append("join(',')");
                    pathBuilderMap.put(parameter.getName(), builder);
                } else if (type instanceof SimpleType) {
                    pathBuilderMap.put(parameter.getName(), builder);
                } else if (type instanceof ObjectType) {
                    for (Property prop : ((ObjectType) type).getProperties().values()) {
                        PathBuilder newBuilder = new PathBuilder(builder);
                        newBuilder.dot().append(prop.getName());
                        Type newType = prop.getType();
                        if (newType instanceof NullableType) {
                            newBuilder.nullable();
                            newType = ((NullableType) newType).getTargetType();
                        }
                        if (newType instanceof ListType) {
                            newBuilder.dot().append("join(',')");
                            pathBuilderMap.put(prop.getName(), newBuilder);
                        } else if (newType instanceof SimpleType) {
                            pathBuilderMap.put(prop.getName(), newBuilder);
                        }
                    }
                }
            }
        }

        boolean hasHeader = operation.getParameters().stream().anyMatch(it -> it.getRequestHeader() != null);
        if (hasHeader) {
            writer.code("const _headers: {[key:string]: string} = ");
            writer.scope(CodeWriter.ScopeType.OBJECT, ", ", false, () -> {
                for (Parameter parameter : operation.getParameters()) {
                    String header = parameter.getRequestHeader();
                    if (header == null || parameter.getType() instanceof NullableType) {
                        continue;
                    }
                    writer.separator().code(header).code(": options.").code(header);
                }
            });
            writer.code(";\n");
        }
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.getRequestHeader() != null) {
                String header = parameter.getRequestHeader();
                Type type = parameter.getType();
                if (type instanceof NullableType) {
                    writer.code("if (options.").code(header).code(") ");
                    writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
                        writer.code("_headers['").code(header).code("'] = options.").code(header).code('\n');
                    }).code('\n');
                }
            } else if (parameter.getRequestParam() != null) {
                PathBuilder builder = new PathBuilder();
                builder.dot().append(parameter.getName());
                Type type = parameter.getType();
                if (type instanceof NullableType) {
                    builder.nullable();
                    type = ((NullableType) type).getTargetType();
                }
                if (type instanceof ListType) {
                    builder.dot().append("join(',')");
                    pathBuilderMap.put(parameter.getName(), builder);
                } else if (type instanceof SimpleType || type instanceof EnumType) {
                    pathBuilderMap.put(parameter.getName(), builder);
                }
            }
        }

        if (!pathBuilderMap.isEmpty()) {
            writer.code("let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n");
            writer.code("let _value: any = undefined;\n");
            for (Map.Entry<String, PathBuilder> e : pathBuilderMap.entrySet()) {
                PathBuilder builder = e.getValue();
                writer.code("_value = options").code(builder.toString()).code(";\n");
                writer.code("if (_value !== undefined && _value !== null) ");
                writer.scope(SourceWriter.ScopeType.OBJECT, "", true, () -> {
                    writer.code("_uri += _separator\n");
                    writer.code("_uri += '").code(e.getKey() + "=").code("'\n");
                    writer.code("_uri += encodeURIComponent(_value);\n");
                    writer.code("_separator = '&';\n");
                });
                writer.code("\n");
            }
        }
        writer.code("return (await this.executor({uri: _uri, method: '")
                .code(operation.getHttpMethods().get(0).name())
                .code("'");
        if (hasHeader) {
            writer.code(", headers: _headers");
        }
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.isRequestBody()) {
                writer.code(", body: options.").code(parameter.getName());
            }
        }
        writer.code("})) as ").typeRef(operation.getReturnType());
    }

    private static Parameter pathVariableParameter(Operation operation, String pathVariable) {
        for (Parameter parameter : operation.getParameters()) {
            if (pathVariable.equals(parameter.getPathVariable())) {
                return parameter;
            }
        }
        throw new IllegalApiException(
                "Illegal operation \"" +
                        operation.getJavaMethod() +
                        "\", the path variable {" +
                        pathVariable +
                        "} cannot be resolved by any parameter"
        );
    }

    private static class UriPart {

        private final static Pattern SLASH_PATTERN = Pattern.compile("\\{(.+?)(?::(?:[^{}]+|\\{[^{}]+?})+)?}");

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
                uriParts.add(new UriPart(matcher.group(1), true));
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
