package org.babyfish.jimmer.sql.api;

import java.util.List;

/**
 * Hello
 * @param <T>
 */
public class Tree<T> {

    /**
     * The first member
     */
    private final String name;

    private final List<Tree<T>> children;

    public Tree(String name, List<Tree<T>> children) {
        this.name = name;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    /**
     * The second member
     */
    public List<Tree<T>> getChildren() {
        return children;
    }
}
