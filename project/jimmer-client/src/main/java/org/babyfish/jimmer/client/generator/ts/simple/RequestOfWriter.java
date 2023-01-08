package org.babyfish.jimmer.client.generator.ts.simple;

import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.generator.ts.TsCodeWriter;
import org.babyfish.jimmer.client.generator.ts.TsContext;

public class RequestOfWriter extends TsCodeWriter {

    public static final File FILE = new File("", "RequestOf");

    public RequestOfWriter(TsContext ctx) {
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
