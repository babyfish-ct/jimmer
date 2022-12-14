package org.babyfish.jimmer.client.generator.ts;

public class File {

    private final String dir;

    private final String name;

    private final boolean isObject;

    File(String dir, String name) {
        this(dir, name, false);
    }

    File(String dir, String name, boolean isObject) {
        if (dir.startsWith("/")) {
            dir = dir.substring(1);
        }
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        this.dir = dir;
        this.name = name;
        this.isObject = isObject;
    }

    public String getDir() {
        return dir;
    }

    public String getName() {
        return name;
    }

    public boolean isObject() {
        return isObject;
    }

    @Override
    public String toString() {
        return dir + '/' + name + ".ts";
    }
}
