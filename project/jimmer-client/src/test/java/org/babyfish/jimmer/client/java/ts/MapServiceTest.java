package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.java.JavaContext;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.service.BookService;
import org.babyfish.jimmer.client.java.service.MapService;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class MapServiceTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParser(new ParameterParserImpl())
                    .setGroups(Collections.singleton("mapService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testService() {
        Context ctx = new TypeScriptContext(METADATA);
        Source source = ctx.getRootSource("services/" + MapService.class.getSimpleName());
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "import type {Executor} from '../';\n" +
                        "\n" +
                        "export class MapService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    readonly findMapBetween: (options: MapServiceOptions['findMapBetween']) => Promise<\n" +
                        "        {readonly [key:string]: any}\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/findBetween/';\n" +
                        "        _uri += encodeURIComponent(options.min);\n" +
                        "        _uri += '/and/';\n" +
                        "        _uri += encodeURIComponent(options.max);\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<{readonly [key:string]: any}>;\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "export type MapServiceOptions = {\n" +
                        "    'findMapBetween': {\n" +
                        "        readonly min: string, \n" +
                        "        readonly max: string\n" +
                        "    }\n" +
                        "}\n",
                writer.toString()
        );
    }
}
