package org.babyfish.jimmer.client.generator.ts.fixed;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;

public class RequestOfRender implements Render {

    private static final String CODE =
            "export type RequestOf<TFuncType> = \n" +
                    "    TFuncType extends (options: infer TRequest) => Promise<any> ? TRequest : never\n" +
                    ";\n";

    @Override
    public void export(SourceWriter writer) {
        writer.code("export type {RequestOf} from './RequestOf';\n");
    }

    @Override
    public void render(SourceWriter writer) {
        writer.code(CODE);
    }
}
