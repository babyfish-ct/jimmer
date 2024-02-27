package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.runtime.NullableType;
import org.babyfish.jimmer.client.runtime.Operation;
import org.babyfish.jimmer.client.runtime.Parameter;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.client.runtime.impl.NullableTypeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OperationRender implements Render {

    private final Operation operation;

    public OperationRender(Operation operation) {
        this.operation = operation;
    }

    @Override
    public void render(SourceWriter writer) {
        if (operation.getReturnType() == null) {
            writer.code("void");
        } else {
            render(operation.getReturnType(), writer);
        }
        writer.code(' ').code(operation.getName());
        writer.scope(CodeWriter.ScopeType.ARGUMENTS, ", ", !operation.getParameters().isEmpty(), () -> {
            for (Parameter parameter : operation.getParameters()) {
                writer.separator();
                render(
                        parameter.getDefaultValue() != null ?
                                NullableTypeImpl.of(parameter.getType()) :
                                parameter.getType(),
                        writer
                );
                writer.code(' ').code(parameter.getName());
            }
        });
        writer.code(";\n");
    }

    private static void render(Type type, SourceWriter writer) {
        if (type instanceof NullableType) {
            String boxedTypeName = Types.boxedTypeName(type);
            if (boxedTypeName != null) {
                writer.code(boxedTypeName);
            } else {
                writer.code('@');
                ((JavaWriter)writer).typeRef(Nullable.class).code(' ');
                writer.typeRef(type);
            }
        } else {
            if (Types.boxedTypeName(type) == null) {
                writer.code('@');
                ((JavaWriter)writer).typeRef(NotNull.class).code(' ');
            }
            writer.typeRef(type);
        }
    }
}
