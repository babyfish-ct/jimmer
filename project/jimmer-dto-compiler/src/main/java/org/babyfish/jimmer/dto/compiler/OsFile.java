package org.babyfish.jimmer.dto.compiler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Abstract of `java.io.File` for test
 */
public interface OsFile {

    String getAbsolutePath();

    Reader openReader() throws IOException;

    static OsFile of(File file) {
        return new OsFile() {
            @Override
            public String getAbsolutePath() {
                return file.getAbsolutePath();
            }

            @Override
            public Reader openReader() throws IOException {
                return new InputStreamReader(
                        Files.newInputStream(file.toPath()),
                        StandardCharsets.UTF_8
                );
            }
        };
    }
}
