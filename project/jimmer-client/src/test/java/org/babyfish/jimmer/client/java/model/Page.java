package org.babyfish.jimmer.client.java.model;

public interface Page<E> extends Slice<E> {

    int getTotalRowCount();

    int getTotalPageCount();
}
