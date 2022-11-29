package org.babyfish.jimmer.sql.meta;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingleColumn implements ColumnDefinition {

    private String name;

    public SingleColumn(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return 1;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Itr(name);
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
