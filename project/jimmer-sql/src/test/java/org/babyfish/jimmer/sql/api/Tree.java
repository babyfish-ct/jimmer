package org.babyfish.jimmer.sql.api;

import java.util.List;

public class Tree<T> {

    private final String name;

    private final List<Tree<T>> children;

    public Tree(String name, List<Tree<T>> children) {
        this.name = name;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public List<Tree<T>> getChildren() {
        return children;
    }
}
