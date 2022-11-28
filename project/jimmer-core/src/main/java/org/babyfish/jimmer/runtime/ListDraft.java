package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.impl.util.Classes;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ListDraft<E> implements List<E>, Draft {

    private final DraftContext ctx;

    private final Class<E> elementType;

    private final List<E> base;

    private List<E> modified;

    private int modCount;

    public ListDraft(Class<E> elementType, List<E> base) {
        this(null, elementType, base);
    }

    @SuppressWarnings("unchecked")
    public ListDraft(DraftContext ctx, Class<E> elementType, List<E> base) {
        this.ctx = ctx;
        this.elementType = (Class<E>) Classes.boxTypeOf(elementType);
        this.base = base;
    }

    public DraftContext draftContext() {
        return ctx;
    }

    @Override
    public boolean isEmpty() {
        return (modified != null ? modified : base).isEmpty();
    }

    @Override
    public int size() {
        return (modified != null ? modified : base).size();
    }

    @Override
    public int indexOf(Object o) {
        return (modified != null ? modified : base).indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return (modified != null ? modified : base).lastIndexOf(o);
    }

    @Override
    public E get(int index) {
        return output((modified != null ? modified : base).get(index));
    }

    @Override
    public boolean contains(Object o) {
        return (modified != null ? modified : base).contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return (modified != null ? modified : base).containsAll(c);
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size()];
        int index = 0;
        for (E e : this) {
            arr[index++] = output(e);
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        int size = size();
        T[] arr = a.length >= size ? a : (T[])new Object[size];
        int index = 0;
        for (E e : this) {
            arr[index++] = (T)output(e);
        }
        return arr;
    }

    @Override
    public boolean add(E e) {
        input(e);
        modCount++;
        return mutable().add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (Object o : c) {
            input(o);
        }
        modCount++;
        return mutable().addAll(c);
    }

    @Override
    public void add(int index, E e) {
        input(e);
        modCount++;
        mutable().add(index, e);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        for (Object o : c) {
            input(o);
        }
        modCount++;
        return mutable().addAll(index, c);
    }

    @Override
    public void clear() {
        if (!isEmpty()) {
            mutable().clear();
            modCount++;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (mutable().remove(o)) {
            modCount++;
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (mutable().removeAll(c)) {
            modCount++;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (mutable().retainAll(c)) {
            modCount++;
            return true;
        }
        return false;
    }

    @Override
    public E set(int index, E element) {
        modCount++;
        return output(mutable().set(index, element));
    }

    @Override
    public E remove(int index) {
        modCount++;
        return output(mutable().remove(index));
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new Itr(0, 0, index, true, null);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        List<E> list = modified != null ? modified : base;
        if (fromIndex < 0 || toIndex > list.size()) {
            throw new IndexOutOfBoundsException();
        }
        return new SubList(fromIndex, list.size() - toIndex);
    }

    private boolean removeRange(int headHide, int tailHide, Predicate<E> predicate) {
        modCount++;
        List<E> m = mutable();
        ListIterator<E> itr = m
                .subList(headHide, m.size() - tailHide)
                .listIterator(m.size() - headHide - tailHide);
        boolean changed = false;
        while (itr.hasPrevious()) {
            E element = itr.previous();
            if (predicate == null || predicate.test(element)) {
                itr.remove();
                changed = true;
            }
        }
        return changed;
    }

    private List<E> mutable() {
        List<E> m = modified;
        if (m == null) {
            modified = m = new ArrayList<>(base);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private E input(Object element) {
        if (element == null || !elementType.isAssignableFrom(element.getClass())) {
            throw new IllegalArgumentException(
                    "New element's type must be \"" +
                            elementType.getName() +
                            "\""
            );
        }
        return (E)element;
    }

    private E output(E element) {
        if (ctx != null) {
            return ctx.toDraftObject(element);
        }
        return element;
    }

    public List<E> resolve() {

        resolveElements();

        List<E> b = base;
        List<E> m = modified;
        if (m == null) {
            return b;
        }
        if (b.size() == m.size()) {
            Iterator<E> itr1 = b.iterator();
            Iterator<E> itr2 = m.iterator();
            boolean changed = false;
            while (!changed && itr1.hasNext() && itr2.hasNext()) {
                if (ctx != null) {
                    changed = !ImmutableSpi.equals(itr1.next(), itr2.next(), true);
                } else {
                    changed = !itr1.next().equals(itr2.next());
                }
            }
            if (!changed) {
                return b;
            }
        }
        return m;
    }

    private void resolveElements() {
        DraftContext ctx = this.ctx;
        if (ctx != null) {
            ListIterator<E> itr = new Itr(0, 0, 0, false, null);
            while (itr.hasNext()) {
                E unresolved = itr.next();
                E resolved = ctx.resolveObject(unresolved);
                if (unresolved != resolved) {
                    itr.set(resolved);
                }
            }
        }
    }

    private class Itr implements ListIterator<E> {
        
        private final int headHide;
        
        private final int tailHide;

        private final boolean outputDraft;
        
        private final Consumer<Integer> modCountChanged;

        private int absIndex;

        private Cursor cursor;

        private int modCount;
        
        private ListIterator<E> baseItr;

        private ListIterator<E> modifiedItr;

        public Itr(
                int headHide,
                int tailHide,
                int index,
                boolean outputDraft,
                Consumer<Integer> modCountChanged
        ) {
            this.headHide = headHide;
            this.tailHide = tailHide;
            this.modCountChanged = modCountChanged;
            this.outputDraft = outputDraft;
            absIndex = headHide + index;

            ListDraft<E> parent = ListDraft.this;
            modCount = parent.modCount;
            baseItr = (parent.modified != null ? parent.modified : parent.base).listIterator(absIndex);
        }

        @Override
        public boolean hasNext() {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            return absIndex < (parent.modified != null ? parent.modified : parent.base).size() - tailHide;
        }

        @Override
        public E next() {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (absIndex >= (parent.modified != null ? parent.modified : parent.base).size() - tailHide) {
                throw new NoSuchElementException();
            }
            cursor = new Cursor(true, absIndex++);
            E next = (modifiedItr != null ? modifiedItr : baseItr).next();
            return outputDraft ? output(next) : next;
        }

        @Override
        public int nextIndex() {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return absIndex - headHide;
        }

        @Override
        public boolean hasPrevious() {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return absIndex > headHide;
        }

        @Override
        public E previous() {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (absIndex <= headHide) {
                throw new NoSuchElementException();
            }
            cursor = new Cursor(false, --absIndex);
            E previous = (modifiedItr != null ? modifiedItr : baseItr).previous();
            return outputDraft ? output(previous) : previous;
        }

        @Override
        public int previousIndex() {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return absIndex - headHide - 1;
        }

        @Override
        public void remove() {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (cursor == null) {
                throw new IllegalStateException();
            }
            int pos = cursor.pos;
            mutableItr().remove();
            if (pos < absIndex) {
                absIndex--;
            }
            cursor = null;
            modCount = ListDraft.this.modCount;
            if (modCountChanged != null) {
                modCountChanged.accept(modCount);
            }
        }

        @Override
        public void add(E element) {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            mutableItr().add(input(element));
            absIndex++;
            cursor = null;
            modCount = ListDraft.this.modCount;
            if (modCountChanged != null) {
                modCountChanged.accept(modCount);
            }
        }

        @Override
        public void set(E element) {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (cursor == null) {
                throw new IllegalStateException();
            }
            mutableItr().set(input(element));
            modCount = ListDraft.this.modCount;
            if (modCountChanged != null) {
                modCountChanged.accept(modCount);
            }
        }

        private ListIterator<E> mutableItr() {
            ListIterator<E> itr = modifiedItr;
            if (modifiedItr == null) {
                if (cursor != null) {
                    itr = cursor.recreate(ListDraft.this.mutable());
                } else {
                    return ListDraft.this.mutable().listIterator(absIndex);
                }
                modifiedItr = itr;
                baseItr = null;
            }
            return itr;
        }
    }

    private class SubList implements List<E> {

        private final int headHide;

        private final int tailHide;

        private int modCount;

        public SubList(int headHide, int tailHide) {
            this.headHide = headHide;
            this.tailHide = tailHide;
            modCount = ListDraft.this.modCount;
        }

        @Override
        public boolean isEmpty() {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return ListDraft.this.size() <= headHide + tailHide;
        }

        @Override
        public int size() {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return ListDraft.this.size() - headHide - tailHide;
        }

        @Override
        public boolean contains(Object element) {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            int absIndex = ListDraft.this.indexOf(element);
            return absIndex >= headHide && absIndex < ListDraft.this.size() - tailHide;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            for (Object element : c) {
                int absIndex = ListDraft.this.indexOf(element);
                if (absIndex < headHide || absIndex >= ListDraft.this.size() - tailHide) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public E get(int index) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (index < 0 || index >= parent.size() - headHide - tailHide) {
                throw new IndexOutOfBoundsException();
            }
            return parent.get(headHide + index);
        }

        @Override
        public int indexOf(Object element) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            int absIndex = parent.indexOf(element);
            if (absIndex >= headHide && absIndex < parent.size() - tailHide) {
                return absIndex - headHide;
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object element) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            int absIndex = parent.lastIndexOf(element);
            if (absIndex >= headHide && absIndex < parent.size() - tailHide) {
                return absIndex - headHide;
            }
            return -1;
        }

        @Override
        public Object[] toArray() {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            Object[] arr = new Object[size()];
            int index = 0;
            for (E e : this) {
                arr[index++] = output(e);
            }
            return arr;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            int size = size();
            T[] arr = a.length >= size ? a : (T[])new Object[size];
            int index = 0;
            for (E e : this) {
                arr[index++] = (T)output(e);
            }
            return arr;
        }

        @Override
        public boolean add(E element) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            parent.add(parent.size() - tailHide, element);
            modCount = parent.modCount;
            return true;
        }

        @Override
        public void add(int index, E element) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (index < 0 || index >= parent.size() - headHide - tailHide) {
                throw new IndexOutOfBoundsException();
            }
            parent.add(index + headHide, element);
            modCount = parent.modCount;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (parent.addAll(parent.size() - tailHide, c)) {
                modCount = parent.modCount;
                return true;
            }
            return false;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (index < 0 || index >= parent.size() - headHide - tailHide) {
                throw new IndexOutOfBoundsException();
            }
            if (parent.addAll(index + headHide, c)) {
                modCount = parent.modCount;
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            parent.removeRange(headHide, tailHide, null);
            modCount = parent.modCount;
        }

        @Override
        public boolean remove(Object element) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (parent.removeRange(
                    headHide,
                    tailHide,
                    it -> Objects.equals(it, element))
            ) {
                modCount = parent.modCount;
                return true;
            }
            return false;
        }

        @Override
        public E remove(int index) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (index < 0 || index >= parent.size() - headHide - tailHide) {
                throw new IndexOutOfBoundsException();
            }
            E result = parent.remove(index + headHide);
            modCount = parent.modCount;
            return output(result);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (parent.removeRange(headHide, tailHide, c::contains)) {
                modCount = parent.modCount;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            ListDraft<E> parent = ListDraft.this;
            if (modCount != parent.modCount) {
                throw new ConcurrentModificationException();
            }
            if (parent.removeRange(headHide, tailHide, it -> !c.contains(it))) {
                modCount = parent.modCount;
                return true;
            }
            return false;
        }

        public E set(int index, E element) {
            ListDraft<E> parent = ListDraft.this;
            if (index < 0 || index >= parent.size() - headHide - tailHide) {
                throw new IndexOutOfBoundsException();
            }
            E result = parent.set(index + headHide, element);
            modCount = parent.modCount;
            return output(result);
        }

        @Override
        public Iterator<E> iterator() {
            return listIterator(0);
        }

        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            if (modCount != ListDraft.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return new Itr(headHide, tailHide, index, true, it -> modCount = it);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            ListDraft<E> parent = ListDraft.this;
            int size = parent.size() - headHide - tailHide;
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException();
            }
            if (fromIndex < 0 || toIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            return new SubList(
                    headHide + fromIndex,
                    tailHide + size - toIndex
            );
        }
    }

    private static class Cursor {

        boolean next;

        int pos;

        public Cursor(boolean next, int pos) {
            this.next = next;
            this.pos = pos;
        }

        public <E> ListIterator<E> recreate(List<E> list) {
            ListIterator<E> itr;
            if (next) {
                itr = list.listIterator(pos);
                itr.next();
            } else {
                itr = list.listIterator(pos + 1);
                itr.previous();
            }
            return itr;
        }
    }
}
