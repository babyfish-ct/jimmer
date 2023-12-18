package org.babyfish.jimmer.client.generator.ts.fixed;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;

public class ElementOfRender implements Render {

    private static final String CODE =
            "export type ElementOf<T> = \n" +
                    "    T extends ReadonlyArray<infer TElement> ? TElement : never\n" +
                    ";\n";

    @Override
    public void export(CodeWriter writer) {
        writer.code("export type {ElementOf} from './ElementOf';\n");
    }

    @Override
    public void render(CodeWriter writer) {
        writer.code(CODE);
    }
}
