package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class IdentityMap<K, V> extends ImNode<K, V> implements Iterable<V> {

    private static final int CAPACITY = 8;

    private ImNode<K, V>[] tab;

    private int modCount;

    public IdentityMap() {
        super(0, null, null, null, null, null);
        before = this;
        after = this;
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (tab == null) {
            tab = new ImNode[CAPACITY];
        }
        int h = System.identityHashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        ImNode<K, V> startNode = tab[index];
        for (ImNode<K, V> node = startNode; node != null; node = node.next) {
            if (node.key == key) {
                V oldValue = node.value;
                node.value = value;
                modCount++;
                return oldValue;
            }
        }
        ImNode<K, V> last = before;
        ImNode<K, V> node = new ImNode<>(h, key, value, startNode, last, this);
        last.after = node;
        before = node;
        tab[index] = node;
        modCount++;
        return null;
    }

    public V get(K key) {
        ImNode<K, V>[] tab = this.tab;
        if (tab == null) {
            return null;
        }
        int h = System.identityHashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        ImNode<K, V> startNode = tab[index];
        for (ImNode<K, V> node = startNode; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return after == this;
    }

    public void replaceAll(Function<V, V> replacer) {
        if (replacer == null || tab == null) {
            return;
        }
        for (ImNode<K, V> node : tab) {
            for (ImNode<K, V> n = node; n != null; n = n.next) {
                n.value = replacer.apply(n.value);
                modCount++;
            }
        }
    }

    public void removeAll(BiPredicate<K, V> predicate) {
        if (predicate == null || after == this) {
            return;
        }
        ImNode<K, V>[] tab = this.tab;
        for (ImNode<K, V> node = this.after; node != this; node = node.after) {
            if (predicate.test(node.key, node.value)) {
                node.before.after = node.after;
                node.after.before = node.before;
                int index = node.hash & (CAPACITY - 1);
                for (ImNode<K, V> p = null, n = tab[index]; n != null; p = n, n = n.next) {
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
        if (after == this) {
            return Collections.emptyIterator();
        }
        return new Itr();
    }

    @Override
    public String toString() {
        if (after == this) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean addComma = false;
        for (ImNode<K, V> n = after; n != this; n = n.after) {
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

        private ImNode<K, V> current;

        public Itr() {
            modCount = IdentityMap.this.modCount;
            current = after;
        }

        @Override
        public boolean hasNext() {
            if (IdentityMap.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            return current != IdentityMap.this;
        }

        @Override
        public V next() {
            if (IdentityMap.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (current == IdentityMap.this) {
                throw new NoSuchElementException();
            }
            V v = current.value;
            current = current.after;
            return v;
        }
    }
}

class ImNode<K, V> {

    final int hash;
    final K key;
    V value;
    ImNode<K, V> next;
    ImNode<K, V> before;
    ImNode<K, V> after;

    ImNode(int hash, K key, V value, ImNode<K, V> next, ImNode<K, V> before, ImNode<K, V> after) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
        this.before = before;
        this.after = after;
    }
}