package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.meta.NullableType;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.client.meta.Parameter;
import org.babyfish.jimmer.client.meta.Service;

import java.io.IOException;

public class ServiceWriter {

    private final Context ctx;

    private final Service service;

    private final File file;

    private final CodeWriter writer;

    public ServiceWriter(Context ctx, Service service, File file) {
        this.ctx = ctx;
        this.service = service;
        this.file = file;
        this.writer = new CodeWriter(ctx, file);
    }

    public void write() throws IOException {
        writer.code("export class ").code(file.getName()).code(' ');
        writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
            for (Operation operation : service.getOperations()) {
                writer.separator();
                writer.code('\n');
                write(operation);
            }
        });
        writer.flush();
    }

    private void write(Operation operation) {
        writer.code(ctx.operationName(operation))
                .scope(CodeWriter.ScopeType.ARGUMENTS, "", false, () -> {
                    if (!operation.getParameters().isEmpty()) {
                        writer.scope(
                                CodeWriter.ScopeType.OBJECT,
                                ", ",
                                operation.getParameters().size() > 2,
                                () -> {
                                    for (Parameter parameter : operation.getParameters().values()) {
                                        writer.separator();
                                        write(parameter);
                                    }
                                }
                        );
                    }
                })
                .code(": ")
                .type(operation.getType())
                .code(' ')
                .scope(CodeWriter.ScopeType.OBJECT, "", true, this::impl);
    }

    private void write(Parameter parameter) {
        writer
                .code("readonly ")
                .code(parameter.getName())
                .codeIf(parameter.getType() instanceof NullableType, '?')
                .code(": ")
                .type(NullableType.unwrap(parameter.getType()));
    }

    private void impl() {

    }
}
