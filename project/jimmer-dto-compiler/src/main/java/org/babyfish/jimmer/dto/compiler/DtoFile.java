package org.babyfish.jimmer.dto.compiler;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public final class DtoFile {

    private final String dtoDir;

    private final String packageName;

    private final String path;

    private final File file;

    public DtoFile(String dtoDir, List<String> paths, File file) {
        this.dtoDir = dtoDir;
        this.packageName = paths.stream().collect(Collectors.joining("."));
        this.path = dtoDir +
                (paths.isEmpty() ? "" : '/' + paths.stream().collect(Collectors.joining("/"))) +
                '/' + file.getName();
        this.file = file;
    }

    public String getDtoDir() {
        return dtoDir;
    }

    public String getPackageName() {
        return packageName;
    }

    public File getFile() {
        return file;
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
