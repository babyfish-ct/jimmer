package org.babyfish.jimmer.client.java.openapi;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.openapi.OpenApiGenerator;
import org.babyfish.jimmer.client.generator.openapi.OpenApiProperties;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class OpenApiGeneratorTest {

    @Test
    public void testBookService() {
        Metadata metadata = Metadata
                .newBuilder()
                .setOperationParser(new OperationParserImpl())
                .setParameterParameter(new ParameterParserImpl())
                .setGroups(Collections.singleton("bookService"))
                .build();
        OpenApiGenerator generator = new OpenApiGenerator(
                metadata,
                OpenApiProperties
                        .newBuilder()
                        .setInfo(
                                OpenApiProperties
                                        .newInfoBuilder()
                                        .setTitle("Book System")
                                        .setDescription("You can use this system the operate book data")
                                        .setVersion("2.0.0")
                                        .build()
                        )
                        .build()
        );
        StringWriter writer = new StringWriter();
        generator.generate(writer);
        System.out.println(writer.toString());
    }

    @Test
    public void testTreeService() {
        Metadata metadata = Metadata
                .newBuilder()
                .setOperationParser(new OperationParserImpl())
                .setParameterParameter(new ParameterParserImpl())
                .setGroups(Collections.singleton("treeService"))
                .build();
        OpenApiGenerator generator = new OpenApiGenerator(metadata, null);
        StringWriter writer = new StringWriter();
        generator.generate(writer);
        System.out.println(writer.toString());
    }
}
