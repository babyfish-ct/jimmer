package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Property;

public class DynamicTypeRender implements Render {

    private final String name;

    private final ObjectType type;

    public DynamicTypeRender(String name, ObjectType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void export(CodeWriter writer) {
        writer.code("export type {").code(name).code("} from './").code(name).code("';\n");
    }

    @Override
    public void render(CodeWriter writer) {
        writer.code("export interface ").code(name).code(' ');
        writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
            for (Property property : type.getProperties().values()) {
                writer.code(property.getName()).code("?: ").typeRef(property.getType()).code("\n");
            }
        });
        writer.code('\n');
    }
}
