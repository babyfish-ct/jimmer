//package org.babyfish.jimmer.client.java.ts;
//
//import org.babyfish.jimmer.client.common.OperationParserImpl;
//import org.babyfish.jimmer.client.common.ParameterParserImpl;
//import org.babyfish.jimmer.client.generator.Context;
//import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
//import org.babyfish.jimmer.client.java.service.RecordService;
//import org.babyfish.jimmer.client.runtime.Metadata;
//import org.babyfish.jimmer.client.source.Source;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//import java.io.StringWriter;
//import java.util.Collections;
//
//public class RecordServiceTest {
//
//    private static final Metadata METADATA =
//            Metadata
//                    .newBuilder()
//                    .setOperationParser(new OperationParserImpl())
//                    .setParameterParser(new ParameterParserImpl())
//                    .setGroups(Collections.singleton("recordService"))
//                    .setGenericSupported(true)
//                    .build();
//
//    @Test
//    public void testService() {
//        Context ctx = new TypeScriptContext(METADATA);
//        Source source = ctx.getRootSource("services/" + RecordService.class.getSimpleName());
//        StringWriter writer = new StringWriter();
//        ctx.render(source, writer);
//        Assertions.assertEquals("import type {Executor} from '../';\n" +
//                        "import type {PageResponse} from '../model/static/';\n" +
//                        "\n" +
//                        "export class RecordService {\n" +
//                        "    \n" +
//                        "    constructor(private executor: Executor) {}\n" +
//                        "    \n" +
//                        "    readonly pageQuery: (options: RecordServiceOptions['pageQuery']) => Promise<\n" +
//                        "        PageResponse<string>\n" +
//                        "    > = async(options) => {\n" +
//                        "        let _uri = '/page/query';\n" +
//                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
//                        "        let _value: any = undefined;\n" +
//                        "        _value = options.pageIndex;\n" +
//                        "        _uri += _separator\n" +
//                        "        _uri += 'pageIndex='\n" +
//                        "        _uri += encodeURIComponent(_value);\n" +
//                        "        _separator = '&';\n" +
//                        "        _value = options.pageSize;\n" +
//                        "        _uri += _separator\n" +
//                        "        _uri += 'pageSize='\n" +
//                        "        _uri += encodeURIComponent(_value);\n" +
//                        "        _separator = '&';\n" +
//                        "        return (await this.executor({uri: _uri, method: 'POST'})) as Promise<PageResponse<string>>;\n" +
//                        "    }\n" +
//                        "}\n" +
//                        "\n" +
//                        "export type RecordServiceOptions = {\n" +
//                        "    'pageQuery': {\n" +
//                        "        readonly pageIndex: number, \n" +
//                        "        readonly pageSize: number\n" +
//                        "    }\n" +
//                        "}\n",
//                writer.toString()
//        );
//    }
//
//
//    @Test
//    public void testPageResponse() {
//        Context ctx = new TypeScriptContext(METADATA);
//        Source source = ctx.getRootSource("model/static/PageResponse");
//        StringWriter writer = new StringWriter();
//        ctx.render(source, writer);
//        Assertions.assertEquals(
//                "export interface PageResponse<R> {\n" +
//                        "    readonly totalRowCount: number;\n" +
//                        "    readonly totalPageCount: number;\n" +
//                        "    readonly rows: ReadonlyArray<R>;\n" +
//                        "}\n",
//                writer.toString()
//        );
//    }
//}
