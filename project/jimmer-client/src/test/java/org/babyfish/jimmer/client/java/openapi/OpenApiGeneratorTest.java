package org.babyfish.jimmer.client.java.openapi;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.openapi.OpenApiGenerator;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

public class OpenApiGeneratorTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParameter(new ParameterParserImpl())
                    .setGroups(Collections.singleton("bookService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void test() throws IOException {
        OpenApiGenerator generator = new OpenApiGenerator(METADATA, null);
        StringWriter writer = new StringWriter();
        generator.generate(writer);
        System.out.println(writer.toString());
    }
}
