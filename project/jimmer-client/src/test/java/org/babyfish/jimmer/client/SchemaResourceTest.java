package org.babyfish.jimmer.client;

import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.impl.MetadataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaResourceTest {

    @TempDir
    Path directory;

    @Test
    public void mergesModelJarsAndRetainsFirstDefinition() throws IOException {
        URL first = jar("first", "{\"services\":[{\"typeName\":\"example.BookApi\",\"groups\":[\"books\"]}]," +
                "\"definitions\":[{\"typeName\":\"example.Page\",\"doc\":{\"value\":\"first\"}}]}");
        URL second = jar("second", "{\"services\":[{\"typeName\":\"example.AuthorApi\",\"groups\":[\"authors\"]}]," +
                "\"definitions\":[{\"typeName\":\"example.Page\",\"doc\":{\"value\":\"second\"}}]}");
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(new URL[] { first, second }, null)) {
            thread.setContextClassLoader(loader);
            Schema all = MetadataBuilder.loadSchema(null);
            assertEquals(new LinkedHashSet<>(Arrays.asList(TypeName.parse("example.BookApi"), TypeName.parse("example.AuthorApi"))),
                    all.getApiServiceMap().keySet());
            assertEquals("first", all.getTypeDefinitionMap().get(TypeName.parse("example.Page")).getDoc().getValue());

            Schema books = MetadataBuilder.loadSchema(Collections.singleton("books"));
            assertEquals(Collections.singleton(TypeName.parse("example.BookApi")), books.getApiServiceMap().keySet());
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    private URL jar(String name, String schema) throws IOException {
        Path file = directory.resolve(name + ".jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
            jar.putNextEntry(new JarEntry("META-INF/jimmer/client"));
            jar.write(schema.getBytes(StandardCharsets.UTF_8));
        }
        return file.toUri().toURL();
    }
}
