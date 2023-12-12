package org.babyfish.jimmer.client.runtime.impl;

import java.util.HashSet;
import java.util.Set;

public abstract class Graph {

    @Override
    public final String toString() {
        return toString(new HashSet<>());
    }

    public final String toString(Set<Graph> stack) {
        if (!stack.add(this)) {
            return "...";
        }
        try {
            return toStringImpl(stack);
        } finally {
            stack.remove(this);
        }
    }

    protected abstract String toStringImpl(Set<Graph> stack);

    protected static String string(Object o, Set<Graph> stack) {
        return ((Graph) o).toString(stack);
    }
}
