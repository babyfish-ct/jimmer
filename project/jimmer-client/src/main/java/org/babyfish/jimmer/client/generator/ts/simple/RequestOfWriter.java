package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.ts.CodeWriter;
import org.babyfish.jimmer.client.generator.ts.Context;
import org.babyfish.jimmer.client.generator.ts.File;

public class RequestOfWriter extends CodeWriter {

    public static final File FILE = new File("", "RequestOf");

    public RequestOfWriter(Context ctx) {
        super(ctx, FILE);
    }

    @Override
    public void write() {

        code("export type RequestOf<TFuncType> = ");
        scope(ScopeType.BLANK, "", true, () -> {
            code("TFuncType extends (options: infer TRequest) => Promise<any> ? ");
            code("TRequest : ");
            code("never");
        });
        code(";\n");
    }
}
