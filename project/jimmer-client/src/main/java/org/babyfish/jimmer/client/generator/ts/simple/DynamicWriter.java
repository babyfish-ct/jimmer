package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.ts.File;
import org.babyfish.jimmer.client.generator.ts.TsCodeWriter;
import org.babyfish.jimmer.client.generator.ts.TsContext;

public class DynamicWriter extends TsCodeWriter {

    public static final File FILE = new File("", "Dynamic");

    public DynamicWriter(TsContext ctx) {
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
