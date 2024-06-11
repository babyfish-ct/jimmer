package org.babyfish.jimmer.sql.meta;

import java.util.Set;

public interface ColumnDefinition extends Storage, Iterable<String> {

    boolean isEmbedded();

    boolean isForeignKey();

    int size();

    String name(int index);

    int index(String name);

    Set<String> toColumnNames();
}
