package org.babyfish.jimmer.sql.ast.impl.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractDataManager<K, V> extends Node<K, V> implements Iterable<V> {

    private static final int CAPACITY = 16;

    private final Node<K, V>[] tab = new Node[CAPACITY];

    protected V getValue(K key) {
        int h = key.hashCode();
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        for (Node<K, V> node = tab[index]; node != null; node = node.next) {
            if (key.equals(node.key)) {
                return node.value;
            }
        }
        return null;
    }

    protected void putValue(K key, V value) {
        int h = key.hashCode();
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        Node<K, V> startNode = tab[index];
        for (Node<K, V> node = startNode; node != null; node = node.next) {
            if (key.equals(node.key)) {
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

    private Node<K, V> createNode(int h, K key, V value, Node<K, V> next) {
        Node<K, V> node = new Node<>(h, key, value, next);
        node.after = this;
        node.before = this.before;
        node.before.after = node;
        node.after.before = node;
        return node;
    }

    private class Itr implements Iterator<V> {
        private Node<K, V> node = AbstractDataManager.this;
        @Override
        public boolean hasNext() {
            return node.after != AbstractDataManager.this;
        }
        @Override
        public V next() {
            Node<K, V> after = node.after;
            if (after == AbstractDataManager.this) {
                throw new NoSuchElementException();
            }
            node = after;
            return after.value;
        }
    }
}

class Node<K, V> {

    final int hash;
    final K key;
    V value;
    Node<K,V> next;
    Node<K, V> before;
    Node<K, V> after;

    Node() {
        hash = 0;
        key = null;
        before = this;
        after = this;
    }

    Node(int hash, K key, V value, Node<K, V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}