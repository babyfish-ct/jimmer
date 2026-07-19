package org.babyfish.jimmer.dto.compiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

public class DtoBundleLoaderTest {

    @TempDir
    private Path tempDir;

    @Test
    public void testExpandedBundle() throws Exception {
        Path root = fixture("root");
        try (URLClassLoader classLoader = classLoader(root)) {
            List<DtoFile> dtoFiles = DtoBundleLoader.load(classLoader);
            Assertions.assertEquals(2, dtoFiles.size());
            assertDtoFile(dtoFiles.get(0), "org.example", "Author.dto", "AuthorView");
            assertDtoFile(dtoFiles.get(1), "org.example", "Book.dto", "BookView");
        }
    }

    @Test
    public void testJarBundleWithCustomPath() throws Exception {
        Path jar = tempDir.resolve("model-dto.jar");
        createJar(fixture("custom-path"), jar);
        try (URLClassLoader classLoader = classLoader(jar)) {
            List<DtoFile> dtoFiles = DtoBundleLoader.load(classLoader);
            Assertions.assertEquals(1, dtoFiles.size());
            assertDtoFile(dtoFiles.get(0), "org.example", "Store.dto", "StoreView");
        }
    }

    @Test
    public void testBundleScanningOption() {
        Assertions.assertTrue(DtoBundleLoader.isEnabled(Collections.emptyMap()));
        Assertions.assertTrue(DtoBundleLoader.isEnabled(Collections.singletonMap(
                DtoBundleLoader.ENABLED_OPTION,
                "true"
        )));
        Assertions.assertFalse(DtoBundleLoader.isEnabled(Collections.singletonMap(
                DtoBundleLoader.ENABLED_OPTION,
                "false"
        )));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DtoBundleLoader.isEnabled(Collections.singletonMap(
                        DtoBundleLoader.ENABLED_OPTION,
                        "invalid"
                ))
        );
    }

    @Test
    public void testUnsupportedMarkerProperty() throws Exception {
        Path marker = tempDir.resolve(DtoBundleLoader.MARKER_PATH);
        Files.createDirectories(marker.getParent());
        Files.write(marker, "unsupported=true".getBytes(StandardCharsets.UTF_8));
        try (URLClassLoader classLoader = classLoader(tempDir)) {
            IOException ex = Assertions.assertThrows(
                    IOException.class,
                    () -> DtoBundleLoader.load(classLoader)
            );
            Assertions.assertTrue(ex.getMessage().contains("unsupported property \"unsupported\""));
        }
    }

    private static void assertDtoFile(
            DtoFile dtoFile,
            String packageName,
            String name,
            String content
    ) throws IOException {
        Assertions.assertEquals(packageName, dtoFile.getPackageName());
        Assertions.assertEquals(name, dtoFile.getName());
        Assertions.assertTrue(read(dtoFile).contains(content));
    }

    private static String read(DtoFile dtoFile) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (Reader reader = dtoFile.openReader()) {
            char[] buffer = new char[256];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, count);
            }
        }
        return builder.toString();
    }

    private static Path fixture(String name) throws Exception {
        URL url = DtoBundleLoaderTest.class
                .getClassLoader()
                .getResource("dto-bundle-fixtures/" + name);
        Assertions.assertNotNull(url);
        return Paths.get(url.toURI());
    }

    private static URLClassLoader classLoader(Path path) throws IOException {
        return new URLClassLoader(new URL[]{path.toUri().toURL()}, null);
    }

    private static void createJar(Path source, Path jar) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar));
             Stream<Path> stream = Files.walk(source)) {
            Iterator<Path> itr = stream
                    .filter(Files::isRegularFile)
                    .iterator();
            URI sourceUri = source.toUri();
            while (itr.hasNext()) {
                Path file = itr.next();
                String name = sourceUri.relativize(file.toUri()).getPath();
                output.putNextEntry(new JarEntry(name));
                Files.copy(file, output);
                output.closeEntry();
            }
        }
    }
}
