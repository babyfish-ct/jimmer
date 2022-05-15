package org.babyfish.jimmer.sql.meta;

public class Column implements Storage {

    private String name;

    public Column(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
