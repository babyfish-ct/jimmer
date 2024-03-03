package org.babyfish.jimmer.sql.ast.impl.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractDataManager<K, V> extends DmNode<K, V> implements Iterable<V> {

    private static final int CAPACITY = 16;

    private final DmNode<K, V>[] tab = new DmNode[CAPACITY];

    protected V getValue(K key) {
        int h = hashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        for (DmNode<K, V> node = tab[index]; node != null; node = node.next) {
            if (equals(key, node.key)) {
                return node.value;
            }
        }
        return null;
    }

    protected void putValue(K key, V value) {
        int h = hashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        DmNode<K, V> startNode = tab[index];
        for (DmNode<K, V> node = startNode; node != null; node = node.next) {
            if (equals(key, node.key)) {
                node.value = value;
                return;
            }
        }
        tab[index] = createNode(h, key, value, startNode);
    }

    public boolean isEmpty() {
        return after == this;
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return new Itr();
    }

    private DmNode<K, V> createNode(int h, K key, V value, DmNode<K, V> next) {
        DmNode<K, V> node = new DmNode<>(h, key, value, next);
        node.after = this;
        node.before = this.before;
        node.before.after = node;
        node.after.before = node;
        return node;
    }

    private class Itr implements Iterator<V> {
        private DmNode<K, V> node = AbstractDataManager.this;
        @Override
        public boolean hasNext() {
            return node.after != AbstractDataManager.this;
        }
        @Override
        public V next() {
            DmNode<K, V> after = node.after;
            if (after == AbstractDataManager.this) {
                throw new NoSuchElementException();
            }
            node = after;
            return after.value;
        }
    }

    protected int hashCode(K key) {
        return key.hashCode();
    }

    protected boolean equals(K key1, K key2) {
        return key1.equals(key2);
    }
}

class DmNode<K, V> {

    final int hash;
    final K key;
    V value;
    DmNode<K,V> next;
    DmNode<K, V> before;
    DmNode<K, V> after;

    DmNode() {
        hash = 0;
        key = null;
        before = this;
        after = this;
    }

    DmNode(int hash, K key, V value, DmNode<K, V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}