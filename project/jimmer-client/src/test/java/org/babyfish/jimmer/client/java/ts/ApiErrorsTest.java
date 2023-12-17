package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.common.OperationParserImpl;
import org.babyfish.jimmer.client.java.common.ParameterParserImpl;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

public class ApiErrorsTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParameter(new ParameterParserImpl())
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testApiErrors() {
        Context ctx = new TypeScriptContext(METADATA);
        Source apiErrorsSources = ctx.getRootSource("ApiErrors");
        StringWriter writer = new StringWriter();
        ctx.render(apiErrorsSources, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }
}
