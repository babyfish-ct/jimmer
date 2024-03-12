package org.babyfish.jimmer.client.java.ts;
//
//import org.babyfish.jimmer.client.common.OperationParserImpl;
//import org.babyfish.jimmer.client.common.ParameterParserImpl;
//import org.babyfish.jimmer.client.generator.Context;
//import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
//import org.babyfish.jimmer.client.java.service.AuthorService;
//import org.babyfish.jimmer.client.runtime.Metadata;
//import org.babyfish.jimmer.client.source.Source;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//import java.io.StringWriter;
//import java.util.Collections;
//
//public class AuthorServiceTest {
//
//    private static final Metadata METADATA =
//            Metadata
//                    .newBuilder()
//                    .setOperationParser(new OperationParserImpl())
//                    .setParameterParameter(new ParameterParserImpl())
//                    .setGroups(Collections.singleton("authorService"))
//                    .setGenericSupported(true)
//                    .build();
//
//    @Test
//    public void testTs() {
//        Context ctx = new TypeScriptContext(METADATA);
//        Source source = ctx.getRootSource("services/" + AuthorService.class.getSimpleName());
//        StringWriter writer = new StringWriter();
//        ctx.render(source, writer);
//        Assertions.assertEquals(
//                "",
//                writer.toString()
//        );
//    }
//}
