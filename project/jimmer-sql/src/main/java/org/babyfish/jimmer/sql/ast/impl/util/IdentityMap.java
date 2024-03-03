package org.babyfish.jimmer.sql.ast.impl.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class IdentityMap<K, V> implements Iterable<V> {

    private static final int CAPACITY = 8;

    private Node<K, V> invalid;

    private Node<K, V>[] tab;

    private int modCount;

    private static class Node<K, V> {

        final int hash;
        final K key;
        V value;
        Node<K, V> next;
        Node<K, V> before;
        Node<K, V> after;

        private Node(int hash, K key, V value, Node<K, V> next, Node<K, V> before, Node<K, V> after) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
            this.before = before != null ? before : this;
            this.after = after != null ? after : this;
        }
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (tab == null) {
            tab = new Node[CAPACITY];
            invalid = new Node<>(0, null, null, null, null, null);
        }
        int h = System.identityHashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        Node<K, V> startNode = tab[index];
        for (Node<K, V> node = startNode; node != null; node = node.next) {
            if (node.key == key) {
                V oldValue = node.value;
                node.value = value;
                modCount++;
                return oldValue;
            }
        }
        Node<K, V> last = invalid.before;
        Node<K, V> node = new Node<>(h, key, value, startNode, last, invalid);
        last.after = node;
        invalid.before = node;
        tab[index] = node;
        modCount++;
        return null;
    }

    public V get(K key) {
        Node<K, V>[] tab = this.tab;
        if (tab == null) {
            return null;
        }
        int h = System.identityHashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        Node<K, V> startNode = tab[index];
        for (Node<K, V> node = startNode; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        Node<K, V> invalid = this.invalid;
        return invalid == null || invalid.after == invalid;
    }

    public void replaceAll(Function<V, V> replacer) {
        if (replacer == null || tab == null) {
            return;
        }
        for (Node<K, V> node : tab) {
            for (Node<K, V> n = node; n != null; n = n.next) {
                n.value = replacer.apply(n.value);
                modCount++;
            }
        }
    }

    public void removeAll(BiPredicate<K, V> predicate) {
        Node<K, V> invalid = this.invalid;
        if (predicate == null || invalid == null) {
            return;
        }
        Node<K, V>[] tab = this.tab;
        for (Node<K, V> node = invalid.after; node != invalid; node = node.after) {
            if (predicate.test(node.key, node.value)) {
                node.before.after = node.after;
                node.after.before = node.before;
                int index = node.hash & (CAPACITY - 1);
                for (Node<K, V> p = null, n = tab[index]; n != null; p = n, n = n.next) {
                    if (node == n) {
                        if (p != null) {
                            p.next = n.next;
                        } else {
                            tab[index] = n.next;
                        }
                        break;
                    }
                }
                modCount++;
            }
        }
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        Node<K, V> invalid = this.invalid;
        if (invalid == null || invalid.after == invalid) {
            return Collections.emptyIterator();
        }
        return new Itr();
    }

    @Override
    public String toString() {
        Node<K, V> invalid = this.invalid;
        if (invalid == null || invalid.after == invalid) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean addComma = false;
        for (Node<K, V> n = invalid.after; n != invalid; n = n.after) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(n.key).append(": ").append(n.value);
        }
        builder.append('}');
        return builder.toString();
    }

    private class Itr implements Iterator<V> {

        private final int modCount;

        private Node<K, V> current;

        public Itr() {
            modCount = IdentityMap.this.modCount;
            current = invalid.after;
        }

        @Override
        public boolean hasNext() {
            if (IdentityMap.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            return current != invalid.after;
        }

        @Override
        public V next() {
            if (IdentityMap.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (current == invalid) {
                throw new NoSuchElementException();
            }
            V v = current.value;
            current = current.after;
            return v;
        }
    }
}
