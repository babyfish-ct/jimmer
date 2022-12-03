package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.impl.DatabaseIdentifiers;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class MultipleColumns implements ColumnDefinition {

    private final String[] arr;

    public MultipleColumns(String[] arr) {
        this.arr = arr;
    }

    @Override
    public int size() {
        return arr.length;
    }

    @Override
    public String name(int index) {
        return arr[index];
    }

    @Override
    public int index(String name) {
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            if (arr[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Itr(arr);
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