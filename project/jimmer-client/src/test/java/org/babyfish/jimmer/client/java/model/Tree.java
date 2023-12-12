package org.babyfish.jimmer.client.java.model;

import java.util.List;

public class Tree<T> {

    private T data;

    private List<Tree<T>> children;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<Tree<T>> getChildren() {
        return children;
    }

    public void setChildren(List<Tree<T>> children) {
        this.children = children;
    }
}
