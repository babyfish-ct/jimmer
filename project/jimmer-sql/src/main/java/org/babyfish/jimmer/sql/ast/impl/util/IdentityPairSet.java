package org.babyfish.jimmer.sql.ast.impl.util;

public class IdentityPairSet<K1, K2> {

    private static final int CAPACITY = 8;

    private final Node<K1, K2>[] tab = new Node[CAPACITY];

    private static class Node<K1, K2> {

        final int hash;
        final K1 key1;
        final K2 key2;
        Node<K1, K2> next;

        private Node(int hash, K1 key1, K2 key2, Node<K1, K2> next) {
            this.hash = hash;
            this.key1 = key1;
            this.key2 = key2;
            this.next = next;
        }
    }

    public boolean has(K1 key1, K2 key2) {
        int h = System.identityHashCode(key1) ^ System.identityHashCode(key2);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        for (Node<K1, K2> node = tab[index]; node != null; node = node.next) {
            if (node.key1 == key1 && node.key2 == key2) {
                return true;
            }
        }
        return false;
    }

    public boolean add(K1 key1, K2 key2) {
        int h = System.identityHashCode(key1) ^ System.identityHashCode(key2);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        Node<K1, K2> startNode = tab[index];
        for (Node<K1, K2> node = startNode; node != null; node = node.next) {
            if (node.key1 == key1 && node.key2 == key2) {
                return false;
            }
        }
        tab[index] = new Node<>(h, key1, key2, startNode);
        return true;
    }
}
