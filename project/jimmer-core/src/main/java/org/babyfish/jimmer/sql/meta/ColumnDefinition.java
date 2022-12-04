package org.babyfish.jimmer.sql.meta;

public interface ColumnDefinition extends Storage, Iterable<String> {

    boolean isEmbedded();

    int size();

    String name(int index);

    int index(String name);
}
