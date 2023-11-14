package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.generator.ts.TsCodeWriter;
import org.babyfish.jimmer.client.generator.ts.TsContext;
import org.babyfish.jimmer.client.meta.Operation;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ExecutorWriter extends TsCodeWriter {

    public static final File FILE = new File("", "Executor");

    public ExecutorWriter(TsContext ctx) {
        super(ctx, FILE, false);
    }

    @Override
    protected void write() {
        code("export type Executor = ");
        scope(ScopeType.BLANK, "", true, () -> {
            code("(args: ");
            scope(ScopeType.OBJECT, ",", true, () -> {
                code("readonly uri: string");
                separator();
                code("readonly method: " + Arrays.stream(Operation.HttpMethod.values()).map(e -> "'" + e.name() + "'").collect(Collectors.joining(" | ")));
                separator();
                code("readonly body?: any");
            });
            code(") => Promise<any>");
        });
        code(";\n");
    }
}
