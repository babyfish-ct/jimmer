package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.NullableType;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.client.meta.Parameter;
import org.babyfish.jimmer.client.meta.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceWriter extends CodeWriter {

    private final Service service;

    public ServiceWriter(Context ctx, Service service) {
        super(ctx, ctx.file(service));
        this.service = service;
    }

    @Override
    protected void write() {
        importFile(new File("", "Executor"));
        code("export class ")
                .code(getFile().getName())
                .scope(ScopeType.ARGUMENTS, "", true, () -> {
                    code("private executor: Executor");
                })
                .code(' ');
        scope(ScopeType.OBJECT, "", true, () -> {
            for (Operation operation : service.getOperations()) {
                separator();
                code('\n');
                write(operation);
            }
        });
    }

    private void write(Operation operation) {
        code("async ").code(getContext().operationName(operation))
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
                });
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
            code("let uri = encodeURIComponent(options.")
                    .code(pathVariableParameter(operation, parts.get(0).text))
                    .code(");\n");
        } else {
            code("let uri = '").code(parts.get(0).text).code("';\n");
        }
        for (int i = 1; i < parts.size(); i++) {
            if (parts.get(i).variable) {
                code("uri += encodeURIComponent(options.")
                        .code(pathVariableParameter(operation, parts.get(i).text))
                        .code(");\n");
            } else {
                code("uri += '").code(parts.get(i).text).code("';\n");
            }
        }

        char separator = operation.getUri().indexOf('?') != -1 ? '&' : '?';
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.getRequestParam() != null) {
                code("uri += '").code(separator).code("';\n");
                code("uri += encodeURIComponent(options." + parameter.getName() + ");\n");
            }
        }
        code("return (await executor({uri, method: '")
                .code(operation.getHttpMethod().name())
                .code("'");
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.isRequestBody()) {
                code(", body: options.").code(parameter.getName());
            }
        }
        code("})) as ").type(operation.getType());
    }

    private static String pathVariableParameter(Operation operation, String pathVariable) {
        for (Parameter parameter : operation.getParameters()) {
            if (pathVariable.equals(parameter.getPathVariable())) {
                return parameter.getName();
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
