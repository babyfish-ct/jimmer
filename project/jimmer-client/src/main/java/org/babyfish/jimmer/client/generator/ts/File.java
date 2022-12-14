package org.babyfish.jimmer.client.generator.ts;

import java.util.Objects;

public class File {

    private final String dir;

    private final String name;

    File(String dir, String name) {
        if (dir.startsWith("/")) {
            dir = dir.substring(1);
        }
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        this.dir = dir;
        this.name = name;
    }

    public String getDir() {
        return dir;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        File file = (File) o;
        return dir.equals(file.dir) && name.equals(file.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dir, name);
    }

    @Override
    public String toString() {
        return dir + '/' + name + ".ts";
    }
}
