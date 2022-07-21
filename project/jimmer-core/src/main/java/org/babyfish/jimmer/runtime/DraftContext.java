package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

public class DraftContext {

    private final IdentityHashMap<Object, Draft> objDraftMap = new IdentityHashMap<>();

    private final IdentityHashMap<List<?>, ListDraft<?>> listDraftMap = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public <D> D toDraftObject(Object obj) {
        if (obj == null || obj instanceof Draft) {
            return (D)obj;
        }
        Draft draft = objDraftMap.get(obj);
        if (draft == null) {
            if (obj instanceof List<?>) {
                throw new IllegalArgumentException("DraftContext.toDraftObject dose not accept list");
            }
            draft = ((ImmutableSpi)obj).__type().getDraftFactory().apply(this, obj);
            objDraftMap.put(obj, draft);
        }
        return (D)draft;
    }

    @SuppressWarnings("unchecked")
    public <E, D> List<D> toDraftList(
            List<E> list,
            Class<E> elementType,
            boolean isElementImmutable
    ) {
        if (list == null || list instanceof Draft) {
            return (List<D>)list;
        }
        ListDraft<?> draft = listDraftMap.get(list);
        if (draft == null) {
            if (isElementImmutable) {
                draft = new ListDraft<>(this, elementType, list);
            } else {
                draft = new ListDraft<>(elementType, list);
            }
            listDraftMap.put(list, draft);
        }
        return (List<D>)draft;
    }

    @SuppressWarnings("unchecked")
    public <E> E resolveObject(E obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof List<?>) {
            throw new IllegalArgumentException("DraftContext.resolveObject dose not accept list");
        }
        Draft draft;
        if (obj instanceof Draft) {
            draft = (Draft)obj;
        } else {
            draft = objDraftMap.get(obj);
        }
        if (draft == null) {
            return obj;
        }
        DraftSpi spi = (DraftSpi)draft;
        if (spi.__draftContext() != this) {
            throw new IllegalArgumentException(
                    "Cannot resolve the draft object because it belong to another draft context"
            );
        }
        return (E)spi.__resolve();
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> resolveList(List<E> list) {
        if (list == null) {
            return null;
        }
        ListDraft<?> draft;
        if (list instanceof Draft) {
            draft = (ListDraft<?>)list;
        } else {
            draft = listDraftMap.get(list);
        }
        if (draft == null) {
            List<E> newList = null;
            int index = 0;
            for (E e : list) {
                E resolved = resolveObject(e);
                if (resolved != e) {
                    if (newList == null) {
                        newList = new ArrayList<>(list.subList(0, index));
                    }
                }
                if (newList != null) {
                    newList.add(resolved);
                }
                index++;
            }
            return newList != null ? newList : list;
        }
        if (draft.draftContext() != null && draft.draftContext() != this) {
            throw new IllegalArgumentException(
                    "Cannot resolve the draft list because it belong to another draft context"
            );
        }
        return (List<E>)draft.resolve();
    }
}
