package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.generator.ts.TsCodeWriter;
import org.babyfish.jimmer.client.generator.ts.TsContext;

public class ElementOfWriter extends TsCodeWriter {

    public static final File FILE = new File("", "ElementOf");

    public ElementOfWriter(TsContext ctx) {
        super(ctx, FILE);
    }

    @Override
    public void write() {

        code("export type ElementOf<T> = ");
        scope(ScopeType.BLANK, "", true, () -> {
            code("T extends ReadonlyArray<infer TElement> ? ");
            code("TElement : ");
            code("never");
        });
        code(";\n");
    }
}
