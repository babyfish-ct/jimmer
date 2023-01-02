package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.ts.CodeWriter;
import org.babyfish.jimmer.client.generator.ts.Context;
import org.babyfish.jimmer.client.generator.ts.File;

public class ElementOfWriter extends CodeWriter {

    public static final File FILE = new File("", "RequestOf");

    public ElementOfWriter(Context ctx) {
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
