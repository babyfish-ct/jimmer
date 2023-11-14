package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.generator.ts.TsCodeWriter;
import org.babyfish.jimmer.client.generator.ts.TsContext;

public class DynamicWriter extends TsCodeWriter {

    public static final File FILE = new File("", "Dynamic");

    public DynamicWriter(TsContext ctx, boolean mutable) {
        super(ctx, FILE, mutable);
    }

    @Override
    public void write() {

        code("export type Dynamic<T> = ");
        scope(ScopeType.BLANK, "", true, () -> {
            code("{")
                    .codeIf(!mutable, "readonly ")
                    .code("[K in keyof T]?: Dynamic<T[K]>}");
        });
        code(";\n");
    }
}
