package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.ts.CodeWriter;
import org.babyfish.jimmer.client.generator.ts.Context;
import org.babyfish.jimmer.client.generator.ts.File;

public class ResponseOfWriter extends CodeWriter {

    public static final File FILE = new File("", "ResponseOf");

    public ResponseOfWriter(Context ctx) {
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
