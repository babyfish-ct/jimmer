package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.Operation;

public class OperationRender implements Render {

    private final String name;

    private final Operation operation;

    public OperationRender(String name, Operation operation) {
        this.name = name;
        this.operation = operation;
    }

    @Override
    public void render(CodeWriter writer) {
        writer
                .code('\n')
                .doc(operation.getDoc(), CodeWriter.DocPart.PARAM, CodeWriter.DocPart.RETURN)
                .code("async ")
                .code(name);
        writer.scope(CodeWriter.ScopeType.ARGUMENTS, ", ", false, () -> {
            writer.code("options: ").code(name).code("Options['").code(name).code("']");
        });
        writer.code(": Promise");
        if (operation.getReturnType() == null) {
            writer.code("<void>");
        } else {
            writer.scope(CodeWriter.ScopeType.GENERIC, ", ", true, () -> {
                writer.typeRef(operation.getReturnType());
            });
        }
        writer.code('\n');
    }
}
