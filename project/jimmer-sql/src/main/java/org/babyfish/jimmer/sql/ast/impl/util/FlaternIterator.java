package org.babyfish.jimmer.sql.ast.impl.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FlaternIterator<E> implements Iterator<E> {

    private final Iterator<List<E>> listItr;

    private Iterator<E> currentItr;

    public FlaternIterator(Iterator<List<E>> listItr) {
        this.listItr = listItr;
        locate();
    }

    @Override
    public boolean hasNext() {
        return currentItr != null;
    }

    @Override
    public E next() {
        Iterator<E> curItr = currentItr;
        if (curItr == null) {
            throw new NoSuchElementException();
        }
        E e = curItr.next();
        if (!curItr.hasNext()) {
            locate();
        }
        return e;
    }

    private void locate() {
        while (listItr.hasNext()) {
            List<E> list = listItr.next();
            if (!list.isEmpty()) {
                currentItr = list.iterator();
                return;
            }
        }
        currentItr = null;
    }
}
