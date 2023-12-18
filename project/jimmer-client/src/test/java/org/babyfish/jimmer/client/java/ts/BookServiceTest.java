package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.common.OperationParserImpl;
import org.babyfish.jimmer.client.java.common.ParameterParserImpl;
import org.babyfish.jimmer.client.java.service.BookService;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class BookServiceTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParameter(new ParameterParserImpl())
                    .setGroups(Collections.singleton("bookService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testService() {
        Context ctx = new TypeScriptContext(METADATA);
        Source serviceSource = ctx.getRootSource("services/" + BookService.class.getSimpleName());
        StringWriter writer = new StringWriter();
        ctx.render(serviceSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testBookDto() {
        Context ctx = new TypeScriptContext(METADATA);
        Source bookDtoSource = ctx.getRootSource("model/dto/BookDto");
        StringWriter writer = new StringWriter();
        ctx.render(bookDtoSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testDynamicBook() {
        Context ctx = new TypeScriptContext(METADATA);
        Source bookDtoSource = ctx.getRootSource("model/dynamic/Dynamic_Book");
        StringWriter writer = new StringWriter();
        ctx.render(bookDtoSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }
}
