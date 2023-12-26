package org.babyfish.jimmer.dto.compiler;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

public final class DtoFile {

    private final File file;

    private final String projectDir;

    private final String dtoDir;

    private final String packageName;

    private final String name;

    private final String path;

    private final ReaderOpener readerOpener;

    public DtoFile(File file, String projectDir, String dtoDir, List<String> packagePaths, String name, ReaderOpener readerOpener) {
        this.file = file;
        this.projectDir = projectDir;
        this.dtoDir = dtoDir;
        this.packageName = String.join(".", packagePaths);
        this.name = name;
        this.path = '<' + projectDir + '>' + '/' + dtoDir +
                (packagePaths.isEmpty() ? "" : '/' + String.join("/", packagePaths)) +
                '/' + name;
        this.readerOpener = readerOpener;
    }

    public File getFile() {
        return file;
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

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    public String getPath() {
        return path;
    }

    public Reader openReader() throws IOException {
        return readerOpener.open();
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

    @FunctionalInterface
    public interface ReaderOpener {
        Reader open() throws IOException;
    }
}
