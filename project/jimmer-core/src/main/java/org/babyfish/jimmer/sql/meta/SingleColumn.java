package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SingleColumn implements ColumnDefinition {

    private final String name;

    private final boolean isForeignKey;

    private final String sqlElementType;

    private final String sqlType;

    public SingleColumn(
            String name,
            boolean isForeignKey,
            String sqlElementType,
            String sqlType
    ) {
        this.name = name;
        this.isForeignKey = isForeignKey;
        this.sqlElementType = sqlElementType != null && !sqlElementType.isEmpty() ? sqlElementType : null;
        this.sqlType = sqlType != null && !sqlType.isEmpty() ? sqlType : null;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isEmbedded() {
        return false;
    }

    @Override
    public boolean isForeignKey() {
        return isForeignKey;
    }

    public String getSqlElementType() {
        return sqlElementType;
    }

    public String getSqlType() {
        return sqlType;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public String name(int index) {
        if (index != 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return name;
    }

    @Override
    public int index(String name) {
        return this.name.equals(name) ? 0 : -1;
    }

    @Override
    public Set<String> toColumnNames() {
        return Collections.singleton(DatabaseIdentifiers.comparableIdentifier(name));
    }

    @Override
    public ColumnDefinition subDefinition(int index) {
        if (index != 0) {
            throw new IllegalArgumentException("Illgal index");
        }
        return this;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Itr(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SingleColumn column = (SingleColumn) o;
        return isForeignKey == column.isForeignKey && name.equals(column.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, isForeignKey);
    }

    @Override
    public String toString() {
        return "SingleColumn{" +
                "name='" + name + '\'' +
                '}';
    }

    private static class Itr implements Iterator<String> {

        private final String name;

        private boolean terminated;

        private Itr(String name) {
            this.name = name;
        }

        @Override
        public boolean hasNext() {
            return !terminated;
        }

        @Override
        public String next() {
            if (terminated) {
                throw new NoSuchElementException();
            }
            terminated = true;
            return name;
        }
    }
}
