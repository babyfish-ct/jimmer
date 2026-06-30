package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.openapi.OpenApiGenerator;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.model.dto.ClientView;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class PolymorphicDtoClientTest {

    private static final Metadata METADATA =
            newMetadata(true);

    private static final Metadata OPEN_API_METADATA =
            newMetadata(false);

    private static Metadata newMetadata(boolean genericSupported) {
        return
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParser(new ParameterParserImpl())
                    .setGroups(Collections.singleton("polymorphicDtoService"))
                    .setGenericSupported(genericSupported)
                    .build();
    }

    @Test
    public void testMetadataBranches() {
        ObjectType type = (ObjectType) METADATA.getType(ClientView.class);
        Assertions.assertEquals(3, type.getPolymorphicBranches().size());
        Assertions.assertEquals(ClientView.Default.class, type.getPolymorphicBranches().get(0).getJavaType());
        Assertions.assertEquals(ClientView.Person.class, type.getPolymorphicBranches().get(1).getJavaType());
        Assertions.assertEquals(ClientView.Organization.class, type.getPolymorphicBranches().get(2).getJavaType());
    }

    @Test
    public void testTypeScript() {
        Context ctx = new TypeScriptContext(METADATA);
        Assertions.assertTrue(
                render(ctx, "model/static/ClientView").contains(
                        "export type ClientView = ClientView_Default | ClientView_Person | ClientView_Organization;"
                )
        );

        String defaultBranch = render(ctx, "model/static/ClientView_Default");
        Assertions.assertTrue(defaultBranch.contains("export interface ClientView_Default"));
        Assertions.assertTrue(defaultBranch.contains("readonly type: ClientType;"));
        Assertions.assertFalse(defaultBranch.contains("firstName"));
        Assertions.assertFalse(defaultBranch.contains("taxCode"));

        String personBranch = render(ctx, "model/static/ClientView_Person");
        Assertions.assertTrue(personBranch.contains("readonly firstName: string;"));
        Assertions.assertFalse(personBranch.contains("taxCode"));

        String organizationBranch = render(ctx, "model/static/ClientView_Organization");
        Assertions.assertTrue(organizationBranch.contains("readonly taxCode: string;"));
        Assertions.assertFalse(organizationBranch.contains("firstName"));
    }

    @Test
    public void testOpenApi() {
        StringWriter writer = new StringWriter();
        new OpenApiGenerator(OPEN_API_METADATA, null).generate(writer);
        String text = writer.toString();
        Assertions.assertTrue(text.contains("ClientView:\n      oneOf:"));
        Assertions.assertTrue(text.contains("$ref: '#/components/schemas/ClientView_Default'"));
        Assertions.assertTrue(text.contains("$ref: '#/components/schemas/ClientView_Person'"));
        Assertions.assertTrue(text.contains("$ref: '#/components/schemas/ClientView_Organization'"));
        Assertions.assertTrue(text.contains("ClientView_Person:\n      type: object"));
        Assertions.assertTrue(text.contains("firstName:"));
        Assertions.assertTrue(text.contains("ClientView_Organization:\n      type: object"));
        Assertions.assertTrue(text.contains("taxCode:"));
    }

    private static String render(Context ctx, String sourceName) {
        Source source = ctx.getRootSource(sourceName);
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        return writer.toString();
    }
}
