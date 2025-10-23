package org.babyfish.jimmer.sql.ast.impl.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public abstract class AbstractDataManager<K, V> extends DmNode<K, V> implements Iterable<V> {

    private static final int CAPACITY = 16;

    private final DmNode<K, V>[] tab = new DmNode[CAPACITY];

    private int size;

    protected final V getValue(K key) {
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

    protected final void putValue(K key, V value) {
        putValue(key, value, null);
    }

    protected final void putValue(K key, V value, BiPredicate<V, V> valueLessBlock) {
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
        DmNode<K, V> insertBefore = this;
        if (valueLessBlock != null) {
            while (true) {
                DmNode<K, V> before = insertBefore.before;
                if (before == this) {
                    break;
                }
                if (!valueLessBlock.test(value, before.value)) {
                    break;
                }
                insertBefore = before;
            }
        }
        tab[index] = createNode(h, key, value, startNode, insertBefore);
        size++;
    }

    public final boolean isEmpty() {
        return size == 0;
    }

    public final int size() {
        return size;
    }

    @NotNull
    @Override
    public final Iterator<V> iterator() {
        return new Itr();
    }

    private static <K, V> DmNode<K, V> createNode(int h, K key, V value, DmNode<K, V> next, DmNode<K, V> after) {
        DmNode<K, V> node = new DmNode<>(h, key, value, next);
        node.after = after;
        node.before = after.before;
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
        public final V next() {
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