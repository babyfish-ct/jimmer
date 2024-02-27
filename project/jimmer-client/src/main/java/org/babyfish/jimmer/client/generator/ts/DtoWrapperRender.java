package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Type;

import java.util.LinkedHashMap;
import java.util.Map;

public class DtoWrapperRender implements Render {

    private final String name;

    public DtoWrapperRender(String name) {
        this.name = name;
    }

    final Map<Type, String> recursiveTypeNames = new LinkedHashMap<>();

    @Override
    public void export(SourceWriter writer) {
        writer.code("export type {").code(name).code("} from './").code(name).code("';\n");
    }

    @Override
    public void render(SourceWriter writer) {
        writer.code("export type ").code(name).code(" = ");
        writer.scope(CodeWriter.ScopeType.OBJECT, ", ", true, writer::renderChildren);
        writer.code('\n');
        for (Map.Entry<Type, String> e : recursiveTypeNames.entrySet()) {
            writer.code("interface ").code(e.getValue()).code(' ');
            FetchedTypeRender.render((ObjectType) e.getKey(), writer, recursiveTypeNames);
            writer.code('\n');
        }
    }
}
