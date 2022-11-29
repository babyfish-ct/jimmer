package org.babyfish.jimmer.sql.meta;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MultipleColumns implements ColumnDefinition {

    private static final String[] EMPTY_ARR = new String[0];

    private final String[] names;

    public MultipleColumns(Collection<String> names) {
        this.names = names.toArray(EMPTY_ARR);
    }

    @Override
    public int size() {
        return names.length;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Itr(names);
    }

    private static class Itr implements Iterator<String> {

        private final String[] arr;

        private int index;

        private Itr(String[] arr) {
            this.arr = arr;
        }

        @Override
        public boolean hasNext() {
            return index < arr.length;
        }

        @Override
        public String next() {
            if (index < arr.length) {
                return arr[index++];
            }
            throw new NoSuchElementException();
        }
    }
}
