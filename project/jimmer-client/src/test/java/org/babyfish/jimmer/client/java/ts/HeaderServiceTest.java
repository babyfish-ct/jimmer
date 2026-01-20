package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.service.HeaderService;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class HeaderServiceTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParser(new ParameterParserImpl())
                    .setGroups(Collections.singleton("headerService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testHyphenNullableHeader() {
        Context ctx = new TypeScriptContext(METADATA);
        Source source = ctx.getRootSource("services/" + HeaderService.class.getSimpleName());
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        String ts = writer.toString();
        Assertions.assertTrue(ts.contains("_headers['Optional-Token'] = options.optionalToken\n"));
        Assertions.assertFalse(ts.contains("_headers[''Optional-Token'']"));
    }
}

