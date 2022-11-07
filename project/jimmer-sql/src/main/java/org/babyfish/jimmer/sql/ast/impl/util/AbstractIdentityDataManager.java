package org.babyfish.jimmer.sql.ast.impl.util;

public abstract class AbstractIdentityDataManager<K, V> {

    private static final int CAPACITY = 8;

    private final Node<K, V>[] tab = new Node[CAPACITY];

    private static class Node<K, V> {

        final int hash;
        final K key;
        V value;
        Node<K,V> next;

        private Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    protected V getValue(K key) {
        int h = System.identityHashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        for (Node<K, V> node = tab[index]; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }

    protected V getOrCreateValue(K key) {
        int h = System.identityHashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        Node<K, V> startNode = tab[index];
        for (Node<K, V> node = startNode; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        V value = createValue(key);
        tab[index] = new Node<>(h, key, value, startNode);
        return value;
    }

    protected void putValue(K key, V value) {
        int h = System.identityHashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        Node<K, V> startNode = tab[index];
        for (Node<K, V> node = startNode; node != null; node = node.next) {
            if (node.key == key) {
                node.value = value;
                return;
            }
        }
        tab[index] = new Node<>(h, key, value, startNode);
    }

    protected abstract V createValue(K key);
}
