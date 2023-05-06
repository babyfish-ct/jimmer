package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class SingleColumn implements ColumnDefinition {

    private final String name;

    private final boolean isForeignKey;

    public SingleColumn(String name, boolean isForeignKey) {
        this.name = name;
        this.isForeignKey = isForeignKey;
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

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Itr(name);
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
