package org.babyfish.jimmer.client.generator.ts.fixed;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;

public class ExecutorRender implements Render {

    private static final String CODE =
            "export type Executor = \n" +
                    "    (args: {\n" +
                    "        readonly uri: string,\n" +
                    "        readonly method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',\n" +
                    "        readonly headers?: {readonly [key:string]: string},\n" +
                    "        readonly body?: any,\n" +
                    "    }) => Promise<any>\n" +
                    ";\n";

    @Override
    public void export(SourceWriter writer) {
        writer.code("export type {Executor} from './Executor';\n");
    }

    @Override
    public void render(SourceWriter writer) {
        writer.code(CODE);
    }
}
