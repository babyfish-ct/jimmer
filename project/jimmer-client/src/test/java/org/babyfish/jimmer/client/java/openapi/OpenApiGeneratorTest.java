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
                new OpenApiProperties()
                        .setInfo(
                                new OpenApiProperties.Info()
                                        .setTitle("Book System")
                                        .setDescription("You can use this system the operate book data")
                                        .setVersion("2.0.0")
                        )
                        .setSecurities(
                                Collections.singletonList(
                                    Collections.singletonMap("tenantHeader", Collections.emptyList())
                                )
                        )
                        .setServers(
                                Collections.singletonList(
                                        new OpenApiProperties.Server()
                                                .setUrl("http://localhost:8080")
                                )
                        )
                        .setComponents(
                                new OpenApiProperties.Components()
                                        .setSecuritySchemes(
                                                Collections.singletonMap(
                                                        "tenantHeader",
                                                        new OpenApiProperties.SecurityScheme()
                                                                .setType("apiKey")
                                                                .setName("tenant")
                                                                .setIn(OpenApiProperties.In.HEADER)
                                                )
                                        )
                        )
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
