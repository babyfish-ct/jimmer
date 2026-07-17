package org.babyfish.jimmer.dto.compiler;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public final class DtoFile {

    private final DtoSource source;

    private final String projectDir;

    private final String dtoDir;

    private final String packageName;

    private final String name;

    private final String path;

    public DtoFile(DtoSource source, String projectDir, String dtoDir, List<String> packagePaths, String name) {
        this.source = source;
        this.projectDir = projectDir;
        this.dtoDir = dtoDir;
        this.packageName = String.join(".", packagePaths);
        this.name = name;
        this.path = '<' + projectDir + '>' +
                (dtoDir.isEmpty() ? "" : '/' + dtoDir) +
                (packagePaths.isEmpty() ? "" : '/' + String.join("/", packagePaths)) +
                '/' + name;
    }

    public String getAbsolutePath() {
        return source.getName();
    }

    public Reader openReader() throws IOException {
        return source.openReader();
    }

    public String getProjectDir() {
        return projectDir;
    }

    public String getDtoDir() {
        return dtoDir;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DtoFile dtoFile = (DtoFile) o;
        return path.equals(dtoFile.path);
    }

    @Override
    public String toString() {
        return path;
    }
}
