package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;

public class ExecutorRender implements Render {

    private static final String CODE =
            "export type Executor = \n" +
                    "    (args: {\n" +
                    "        readonly uri: string,\n" +
                    "        readonly method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',\n" +
                    "        readonly body?: any\n" +
                    "    }) => Promise<any>\n" +
                    ";\n";

    @Override
    public void render(CodeWriter writer) {
        writer.code(CODE);
    }
}
