package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.runtime.impl.IllegalApiException;
import org.babyfish.jimmer.client.runtime.impl.NullableTypeImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OperationRender implements Render {

    private static final Pattern SIMPLE_IDENTIFIER_PATTERN =
            Pattern.compile("^[A-Za-z_$][0-9A-Za-z_$]*$");

    private final String name;

    private final Operation operation;

    public OperationRender(String name, Operation operation) {
        this.name = name;
        this.operation = operation;
    }

    @Override
    public void render(SourceWriter writer) {
        renderDoc(writer);
        writer
                .code("readonly ")
                .code(name)
                .code(": ");
        if (!operation.getParameters().isEmpty()) {
            writer.scope(SourceWriter.ScopeType.ARGUMENTS, ", ", false, () -> {
                writer
                        .code("options: ")
                        .code(writer.getSource().getRoot().getName()).code("Options['").code(name).code("']");
            });
        } else {
            writer.code("()");
        }
        writer.code(" => Promise");
        writer.scope(SourceWriter.ScopeType.GENERIC, ", ", true, () -> {
            if (operation.getReturnType() == null) {
                writer.code("void");
            } else {
                writer.typeRef(operation.getReturnType());
            }
        });
        writer.code(" = ");
        writer.code("async(").codeIf(!operation.getParameters().isEmpty(), "options").code(") => ");
        writer.scope(SourceWriter.ScopeType.OBJECT, "", true, () -> {
            renderImpl(writer);
        });
        writer.code('\n');
    }

    private void renderDoc(SourceWriter writer) {
        if (operation.getDoc() != null) {
            Doc doc = operation.getDoc();
            writer.code("/**\n");
            if (doc.getValue() != null) {
                writer.code(" * ").code(doc.getValue().replace("\n", "\n * ")).code('\n');
            }
            Map<String, String> parameterMap = doc.getParameterValueMap();
            if (parameterMap != null && !parameterMap.isEmpty()) {
                writer.code(" * @parameter {")
                        .code(writer.getSource().getRoot().getName()).code("Options['").code(name).code("']")
                        .code("} options\n");
                for (Map.Entry<String, String> e : parameterMap.entrySet()) {
                    writer.code(" * - ")
                            .code(e.getKey()).code(' ')
                            .code(e.getValue().replace("\n", "\n * "))
                            .code('\n');
                }
            }
            if (doc.getReturnValue() != null) {
                writer.code(" * @return ")
                        .code(doc.getReturnValue().replace("\n", "\n * "))
                        .code('\n');
            }
            writer.code(" */\n");
        }
    }

    private void renderImpl(SourceWriter writer) {
        List<UriPart> uriParts = UriPart.parts(operation.getUri());

        for (int i = 0; i < uriParts.size(); i++) {
            writer.code(i == 0 ? "let _uri = " : "_uri += ");
            if (uriParts.get(i).variable) {
                Parameter parameter = pathVariableParameter(operation, uriParts.get(i).text);
                writer.code("encodeURIComponent(options.")
                        .code(parameter.getName())
                        .code(");\n");
            } else {
                writer.code("'").code(uriParts.get(i).text).code("';\n");
            }
        }

        Map<String, PathBuilder> pathBuilderMap = new LinkedHashMap<>();
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.getPathVariable() == null &&
                    parameter.getRequestParam() == null &&
                    parameter.getRequestHeader() == null &&
                    parameter.getRequestPart() == null &&
                    !parameter.isRequestBody()) {
                PathBuilder builder = new PathBuilder();
                builder.dot().append(parameter.getName());
                Type type = parameter.getType();
                if (type instanceof NullableType) {
                    builder.nullable();
                    type = ((NullableType) type).getTargetType();
                }
                if (type instanceof ListType) {
                    builder.array();
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
                            newBuilder.array();
                            pathBuilderMap.put(prop.getName(), newBuilder);
                        } else if (newType instanceof SimpleType) {
                            pathBuilderMap.put(prop.getName(), newBuilder);
                        } else if (newType instanceof EnumType) {
                            pathBuilderMap.put(prop.getName(), newBuilder);
                        } else if (newType instanceof TypeVariable) {
                            writer.code(((TypeVariable)newType).getName());
                        } else {
                            throw new IllegalApiException(
                                    "Illegal java method \"" +
                                            operation.getJavaMethod() +
                                            "\", its parameter \"" +
                                            parameter.getName() +
                                            "\" is object type, however, it is not request body and the deeper property \"" +
                                            prop.getName() +
                                            "\" is not simple type"
                            );
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
                    writer.separator().code(tsObjectKey(header)).code(": options.").code(parameter.getName());
                }
            });
            writer.code(";\n");
        }
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.getRequestHeader() != null) {
                String header = parameter.getRequestHeader();
                Type type = parameter.getType();
                if (type instanceof NullableType) {
                    writer.code("if (options.").code(parameter.getName()).code(") ");
                    writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
                        writer.code("_headers[").code(tsStringLiteral(header)).code("] = options.").code(parameter.getName()).code('\n');
                    }).code('\n');
                }
            } else if (parameter.getRequestParam() != null) {
                PathBuilder builder = new PathBuilder();
                builder.dot().append(parameter.getName());
                Type type = parameter.getType();
                if (parameter.getDefaultValue() != null) {
                    builder.nullable();
                }
                if (type instanceof NullableType) {
                    builder.nullable();
                    type = ((NullableType) type).getTargetType();
                }
                if (type instanceof ListType) {
                    builder.array();
                    pathBuilderMap.put(parameter.getRequestParam(), builder);
                } else if (type instanceof SimpleType || type instanceof EnumType) {
                    pathBuilderMap.put(parameter.getRequestParam(), builder);
                }
            }
        }

        if (!pathBuilderMap.isEmpty()) {
            writer.code("let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n");
            writer.code("let _value: any = undefined;\n");
            for (Map.Entry<String, PathBuilder> e : pathBuilderMap.entrySet()) {
                PathBuilder builder = e.getValue();
                writer.code("_value = options").code(builder.toString()).code(";\n");
                if (builder.nullable) {
                    writer.code("if (_value !== undefined && _value !== null) ");
                    writer.scope(SourceWriter.ScopeType.OBJECT, "", true, () -> {
                        if (builder.isArray()) {
                            writer.code("for (const _item of _value) ");
                            writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
                                writer.code("_uri += _separator\n");
                                writer.code("_uri += '").code(e.getKey() + "=").code("'\n");
                                writer.code("_uri += encodeURIComponent(_item);\n");
                                writer.code("_separator = '&';\n");
                            });
                            writer.code("\n");
                        } else {
                            writer.code("_uri += _separator\n");
                            writer.code("_uri += '").code(e.getKey() + "=").code("'\n");
                            writer.code("_uri += encodeURIComponent(_value);\n");
                            writer.code("_separator = '&';\n");
                        }
                    });
                    writer.code("\n");
                } else {
                    if (builder.isArray()) {
                        writer.code("for (const _item of _value) ");
                        writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
                            writer.code("_uri += _separator\n");
                            writer.code("_uri += '").code(e.getKey() + "=").code("'\n");
                            writer.code("_uri += encodeURIComponent(_item);\n");
                            writer.code("_separator = '&';\n");
                        });
                        writer.code("\n");
                    } else {
                        writer.code("_uri += _separator\n");
                        writer.code("_uri += '").code(e.getKey() + "=").code("'\n");
                        writer.code("_uri += encodeURIComponent(_value);\n");
                        writer.code("_separator = '&';\n");
                    }
                }
            }
        }

        List<Parameter> requestPartParameters = operation
                .getParameters()
                .stream()
                .filter(it -> it.getRequestPart() != null)
                .collect(Collectors.toList());
        if (!requestPartParameters.isEmpty()) {
            writer.code("const _formData = new FormData();\n");
            writer.code("const _body = options.body;\n");
            boolean notNull = requestPartParameters.stream().anyMatch(p -> !(p.getType() instanceof NullableType));
            if (notNull) {
                for (Parameter parameter : requestPartParameters) {
                    if (parameter.getType() instanceof NullableType) {
                        writer.code("if (_body.").code(parameter.getName()).code(") ");
                        writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
                            renderRequestPart(parameter, writer);
                        }).code('\n');
                    } else {
                        renderRequestPart(parameter, writer);
                    }
                }
            } else {
                writer.code("if (_body) ").scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
                    for (Parameter parameter : requestPartParameters) {
                        if (parameter.getType() instanceof NullableType) {
                            writer.code("if (_body.").code(parameter.getName()).code(") ");
                            writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
                                renderRequestPart(parameter, writer);
                            }).code('\n');
                        } else {
                            renderRequestPart(parameter, writer);
                        }
                    }
                });
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
                writer.code(", body: options.body");
            }
        }
        if (!requestPartParameters.isEmpty()) {
            writer.code(", body: _formData");
        }
        writer.code("})) as Promise<");
        if (operation.getReturnType() == null) {
            writer.code("void");
        } else {
            writer.typeRef(operation.getReturnType());
        }
        writer.code(">;");
    }

    private static String tsObjectKey(String name) {
        if (SIMPLE_IDENTIFIER_PATTERN.matcher(name).matches()) {
            return name;
        }
        return tsStringLiteral(name);
    }

    private static String tsStringLiteral(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('\'');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\'':
                    builder.append("\\'");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\u2028':
                    builder.append("\\u2028");
                    break;
                case '\u2029':
                    builder.append("\\u2029");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        builder.append('\'');
        return builder.toString();
    }

    private void renderRequestPart(Parameter parameter, SourceWriter writer) {
        Type type = NullableTypeImpl.unwrap(parameter.getType());
        if (type instanceof VirtualType.File) {
            writer.code("_formData.append(\"")
                    .code(parameter.getRequestPart())
                    .code("\", _body.")
                    .code(parameter.getName())
                    .code(");\n");
        } else if (type instanceof ListType && NullableTypeImpl.unwrap(((ListType)type).getElementType()) instanceof VirtualType.File) {
            writer.code("for (const file of _body.").code(parameter.getName()).code(") ");
            writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
                writer.code("_formData.append(\"")
                        .code(parameter.getRequestPart())
                        .code("\", file);\n");
            }).code('\n');
        } else {
            writer.code("_formData.append").scope(CodeWriter.ScopeType.ARGUMENTS, ", ", true, () -> {
                writer.code('"').code(parameter.getRequestPart()).code('"');
                writer.separator();
                writer.code("new Blob").scope(CodeWriter.ScopeType.ARGUMENTS, ", ", true, () -> {
                    writer.code("[JSON.stringify(_body.").code(parameter.getName()).code(")]");
                    writer.separator();
                    writer.code("{type: \"application/json\"}");
                });
            }).code(";\n");
        }
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

        private boolean array;

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

        public PathBuilder array() {
            this.array = true;
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

        public boolean isArray() {
            return array;
        }

        public String toString() {
            return builder.toString();
        }
    }
}
