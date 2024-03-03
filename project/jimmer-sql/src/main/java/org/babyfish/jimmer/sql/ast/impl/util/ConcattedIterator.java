package org.babyfish.jimmer.sql.ast.impl.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ConcattedIterator<E> implements Iterator<E> {

    private final Iterator<E>[] iterators;

    private int index;

    private Iterator<E> currentItr;

    private ConcattedIterator(Iterator<E>[] iterators) {
        this.iterators = iterators;
        locate();
    }

    @SafeVarargs
    public static <E> Iterator<E> of(Iterator<E>... iterators) {
        return new ConcattedIterator<>(iterators);
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
        for (Iterator<E> itr : iterators) {
            if (itr.hasNext()) {
                currentItr = itr;
                return;
            }
        }
        currentItr = null;
    }
}
