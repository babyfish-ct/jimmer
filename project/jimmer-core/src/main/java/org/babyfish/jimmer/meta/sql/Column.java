package org.babyfish.jimmer.meta.sql;

public class Column implements Storage {

    private String name;

    public Column(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
