package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.ts.File;
import org.babyfish.jimmer.client.generator.ts.TsCodeWriter;
import org.babyfish.jimmer.client.generator.ts.TsContext;

public class ResponseOfWriter extends TsCodeWriter {

    public static final File FILE = new File("", "ResponseOf");

    public ResponseOfWriter(TsContext ctx) {
        super(ctx, FILE);
    }

    @Override
    public void write() {

        code("export type ResponseOf<TFuncType> = ");
        scope(ScopeType.BLANK, "", true, () -> {
            code("TFuncType extends (options: any) => Promise<infer TResponse> ? ");
            code("TResponse : ");
            code("never");
        });
        code(";\n");
    }
}
