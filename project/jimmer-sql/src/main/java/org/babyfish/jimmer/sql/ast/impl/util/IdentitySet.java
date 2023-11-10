package org.babyfish.jimmer.sql.ast.impl.util;

public class IdentitySet<E> {

    private static final int CAPACITY = 8;

    private final Node<E>[] tab = new Node[CAPACITY];

    private static class Node<E> {

        final int hash;
        final E key;
        Node<E> next;

        private Node(int hash, E key, Node<E> next) {
            this.hash = hash;
            this.key = key;
            this.next = next;
        }
    }

    public boolean add(E key) {
        int h = System.identityHashCode(key);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        Node<E> startNode = tab[index];
        for (Node<E> node = startNode; node != null; node = node.next) {
            if (node.key == key) {
                return false;
            }
        }
        tab[index] = new Node<>(h, key, startNode);
        return true;
    }
}
