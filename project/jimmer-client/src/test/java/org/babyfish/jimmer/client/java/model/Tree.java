package org.babyfish.jimmer.client.java.model;

import java.util.List;

/**
 * Static Object Tree
 * @param <T> The data type of each node
 */
public class Tree<T> {

    /**
     * The data of tree node
     */
    private T data;

    private List<Tree<T>> children;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * Get child trees
     * @return The child trees of current node
     */
    public List<Tree<T>> getChildren() {
        return children;
    }

    public void setChildren(List<Tree<T>> children) {
        this.children = children;
    }
}
