package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ServiceWriter extends CodeWriter {

    private final Service service;

    public ServiceWriter(Context ctx, Service service) {
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
    }

    private void write(Operation operation) {
        code('\n');
        document(operation.getDocument());
        code("async ").code(getContext().getOperationName(operation))
                .scope(ScopeType.ARGUMENTS, "", false, () -> {
                    if (!operation.getParameters().isEmpty()) {
                        code("options: ");
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
                })
                .code(": Promise<")
                .type(operation.getType())
                .code("> ")
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
                .type(NullableType.unwrap(parameter.getType()));
    }

    private void impl(Operation operation) {
        List<UriPart> parts = UriPart.parts(operation.getUri());
        if (parts.get(0).variable) {
            Parameter parameter = pathVariableParameter(operation, parts.get(0).text);
            code("let uri = encodeURIComponent(options.")
                    .code(parameter.getName())
                    .codeIf(parameter.getType() instanceof ArrayType, "join(',')")
                    .code(");\n");
        } else {
            code("let uri = '").code(parts.get(0).text).code("';\n");
        }
        for (int i = 1; i < parts.size(); i++) {
            if (parts.get(i).variable) {
                Parameter parameter = pathVariableParameter(operation, parts.get(i).text);
                code("uri += encodeURIComponent(options.")
                        .code(parameter.getName())
                        .codeIf(parameter.getType() instanceof ArrayType, "join(',')")
                        .code(");\n");
            } else {
                code("uri += '").code(parts.get(i).text).code("';\n");
            }
        }

        List<Parameter> urlParameters = operation
                .getParameters()
                .stream()
                .filter(it -> it.getRequestParam() != null)
                .collect(Collectors.toList());
        if (!urlParameters.isEmpty()) {
            boolean hasParamStart = operation.getUri().indexOf('?') != -1;
            boolean dynamicSeparator = !hasParamStart &&
                    urlParameters.get(0).getType() instanceof NullableType &&
                    urlParameters.size() > 1;
            String sp;
            if (dynamicSeparator) {
                code("let separator = '?';\n");
                sp = "separator";
            } else {
                sp = hasParamStart ? "&" : "?";
            }
            for (Parameter parameter : operation.getParameters()) {
                if (parameter.getRequestParam() != null) {
                    final String finalSp = sp;
                    final boolean finalDynamic = dynamicSeparator;
                    Runnable addUrlParameter = () -> {
                        if (finalDynamic) {
                            code("uri += ").code(finalSp).code(";\n");
                            code("uri += '").code(parameter.getRequestParam()).code("=';\n");
                        } else {
                            code("uri += '").code(finalSp).code(parameter.getRequestParam()).code("=';\n");
                        }
                        code("uri += encodeURIComponent(options." + parameter.getName() + ");\n");
                        if (finalDynamic && parameter.getType() instanceof NullableType) {
                            code("separator = '&';\n");
                        }
                    };
                    if (parameter.getType() instanceof NullableType) {
                        code("if (options.")
                                .code(parameter.getName()).code(" !== undefined && options.")
                                .code(parameter.getName()).code(" !== null) ");
                        scope(ScopeType.OBJECT, "", true, addUrlParameter);
                        code('\n');
                    } else {
                        addUrlParameter.run();
                        dynamicSeparator = false;
                    }
                    if (!dynamicSeparator) {
                        sp = "&";
                    }
                }
            }
        }
        code("return (await this.executor({uri, method: '")
                .code(operation.getHttpMethod().name())
                .code("'");
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.isRequestBody()) {
                code(", body: options.").code(parameter.getName());
            }
        }
        code("})) as ").type(operation.getType());
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
                        "} cannot be resolved any any parameters"
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
}
