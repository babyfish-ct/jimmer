package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.ts.CodeWriter;
import org.babyfish.jimmer.client.generator.ts.Context;
import org.babyfish.jimmer.client.generator.ts.File;

public class DynamicWriter extends CodeWriter {

    public static final File FILE = new File("", "Dynamic");

    public DynamicWriter(Context ctx) {
        super(ctx, FILE);
    }

    @Override
    public void write() {

        code("export type Dynamic<T> = ");
        scope(ScopeType.BLANK, "", true, () -> {
            code("{[K in keyof T]?: Dynamic<T[K]>}");
        });
        code(";\n");
    }
}
