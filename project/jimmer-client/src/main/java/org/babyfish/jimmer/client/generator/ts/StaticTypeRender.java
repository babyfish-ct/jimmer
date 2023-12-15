package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Property;
import org.babyfish.jimmer.client.runtime.Type;

public class StaticTypeRender implements Render {

    private final String name;

    private final ObjectType type;

    public StaticTypeRender(String name, ObjectType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void render(CodeWriter writer) {
        writer.code("export interface ").code(name);
        if (!type.getArguments().isEmpty()) {
            writer.scope(CodeWriter.ScopeType.GENERIC, ", ", false, () -> {
                for (Type argument : type.getArguments()) {
                    writer.typeRef(argument);
                }
            });
        }
        TypeScriptContext ctx = writer.getContext();
        writer.code(' ').scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
            for (Property property : type.getProperties().values()) {
                writer
                        .codeIf(ctx.isMutable(), "readonly ")
                        .code(property.getName())
                        .code(": ")
                        .typeRef(property.getType())
                        .code(";\n");
            }
        });
        writer.code('\n');
    }
}
