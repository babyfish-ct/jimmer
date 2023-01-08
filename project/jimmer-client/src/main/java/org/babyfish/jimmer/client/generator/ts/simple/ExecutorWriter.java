package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.ts.File;
import org.babyfish.jimmer.client.generator.ts.TsCodeWriter;
import org.babyfish.jimmer.client.generator.ts.TsContext;

public class ExecutorWriter extends TsCodeWriter {

    public static final File FILE = new File("", "Executor");

    public ExecutorWriter(TsContext ctx) {
        super(ctx, FILE);
    }

    @Override
    protected void write() {
        code("export type Executor = ");
        scope(ScopeType.BLANK, "", true, () -> {
            code("(args: ");
            scope(ScopeType.OBJECT, ",", true, () -> {
                code("readonly uri: string");
                separator();
                code("readonly method: 'GET' | 'POST' | 'PUT' | 'DELETE'");
                separator();
                code("readonly body?: any");
            });
            code(") => Promise<any>");
        });
        code(";\n");
    }
}
