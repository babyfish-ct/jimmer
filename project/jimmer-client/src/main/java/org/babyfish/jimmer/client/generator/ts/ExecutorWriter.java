package org.babyfish.jimmer.client.generator.ts;

public class ExecutorWriter extends CodeWriter {

    public static final File FILE = new File("", "Executor");

    public ExecutorWriter(Context ctx) {
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
