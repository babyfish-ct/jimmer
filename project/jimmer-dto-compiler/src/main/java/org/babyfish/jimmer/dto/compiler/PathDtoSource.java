package org.babyfish.jimmer.dto.compiler;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathDtoSource implements DtoSource {

    private final Path path;

    public PathDtoSource(Path path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return path.toUri().toString();
    }

    @Override
    public Reader openReader() throws IOException {
        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }
}
