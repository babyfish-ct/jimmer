package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.common.OperationParserImpl;
import org.babyfish.jimmer.client.java.common.ParameterParserImpl;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IndexTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParameter(new ParameterParserImpl())
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testRoot() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("", writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testServices() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("services", writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testModelDto() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("model/dto", writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testModelDynamic() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("model/dynamic", writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testModelStatic() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("model/static", writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void tmp() throws IOException {
        Context ctx = new TypeScriptContext(METADATA);
        ctx.renderAll(Files.newOutputStream(Paths.get("/Users/chentao/tmp/new-ts.zip")));
    }
}
