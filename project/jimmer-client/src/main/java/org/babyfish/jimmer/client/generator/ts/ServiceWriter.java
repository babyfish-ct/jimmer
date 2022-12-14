package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.meta.NullableType;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.client.meta.Parameter;
import org.babyfish.jimmer.client.meta.Service;

public class ServiceWriter extends CodeWriter {

    private final Service service;

    public ServiceWriter(Context ctx, Service service) {
        super(ctx, ctx.file(service));
        this.service = service;
    }

    @Override
    protected void write() {
        code("export class ").code(getFile().getName()).code(' ');
        scope(ScopeType.OBJECT, "", true, () -> {
            for (Operation operation : service.getOperations()) {
                separator();
                code('\n');
                write(operation);
            }
        });
    }

    private void write(Operation operation) {
        code(getContext().operationName(operation))
                .scope(ScopeType.ARGUMENTS, "", false, () -> {
                    if (!operation.getParameters().isEmpty()) {
                        scope(
                                ScopeType.OBJECT,
                                ", ",
                                operation.getParameters().size() > 2,
                                () -> {
                                    for (Parameter parameter : operation.getParameters().values()) {
                                        separator();
                                        write(parameter);
                                    }
                                }
                        );
                    }
                })
                .code(": ")
                .type(operation.getType())
                .code(' ')
                .scope(ScopeType.OBJECT, "", true, this::impl);
    }

    private void write(Parameter parameter) {
        code("readonly ")
                .code(parameter.getName())
                .codeIf(parameter.getType() instanceof NullableType, '?')
                .code(": ")
                .type(NullableType.unwrap(parameter.getType()));
    }

    private void impl() {

    }

    @Override
    protected boolean rawImmutableAsDynamic() {
        return true;
    }
}
