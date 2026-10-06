package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.CircularReferenceException;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.sql.collection.AbstractIdViewList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;

public class DraftContext {

    private final DraftContext parent;

    @Nullable
    private IdentityHashMap<Object, Draft> objDraftMap;

    private DisposerHolder disposerHolder;

    public DraftContext(DraftContext parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    public <D> D toDraftObject(Object obj) {
        if (obj == null || obj instanceof Draft) {
            return (D) obj;
        }
        IdentityHashMap<Object, Draft> objDraftMap = this.objDraftMap;
        Draft draft = objDraftMap != null ? objDraftMap.get(obj) : null;
        if (draft == null) {
            if (obj instanceof List<?>) {
                throw new IllegalArgumentException("DraftContext.toDraftObject dose not accept list");
            }
            draft = ((ImmutableSpi) obj).__type().getDraftFactory().apply(this, obj);
            if (objDraftMap == null) {
                objDraftMap = this.objDraftMap = new IdentityHashMap<>(1);
            }
            objDraftMap.put(obj, draft);
        }
        return (D) draft;
    }

    @SuppressWarnings("unchecked")
    public <E, D> List<D> toDraftList(
            DraftSpi owner,
            PropId propId,
            List<E> list,
            Class<E> elementType,
            boolean isElementImmutable
    ) {
        if (list == null || list instanceof AbstractIdViewList<?, ?>) {
            return (List<D>) list;
        }
        ListDraft<?> draft = owner.__getListDraft(propId);
        if (draft == null || !draft.isBase(list)) {
            draft = isElementImmutable ? new ListDraft<>(this, elementType, list) : new ListDraft<>(elementType, list);
            owner.__setListDraft(propId, draft);
        }
        return (List<D>) draft;
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> resolveList(DraftSpi owner, PropId propId, List<E> list) {
        if (list == null) {
            return null;
        }
        ListDraft<?> draft = owner.__getListDraft(propId);
        if (draft != null && draft.isBase(list)) {
            return ListUtils.unmodifiable((List<E>) draft.resolve());
        }
        return ListUtils.unmodifiable(resolveList(list));
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
            draft = (Draft) obj;
        } else if (obj instanceof ImmutableSpi) {
            IdentityHashMap<Object, Draft> objDraftMap = this.objDraftMap;
            draft = objDraftMap != null ? objDraftMap.get(obj) : null;
        } else {
            draft = null;
        }
        if (draft == null) {
            return obj;
        }
        DraftSpi spi = (DraftSpi) draft;
        if (spi.__isResolved()) {
            // Issue #597
            return (E) spi.__resolve();
        }
        validateOtherDraft(
                spi.__draftContext(),
                "Cannot resolve the draft object because it belong to another draft context"
        );
        return (E) spi.__resolve();
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> resolveList(List<E> list) {
        if (list == null) {
            return null;
        }
        ListDraft<?> draft;
        if (list instanceof Draft) {
            draft = (ListDraft<?>) list;
        } else {
            draft = null;
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
        validateOtherDraft(
                draft.draftContext(),
                "Cannot resolve the draft list because it belong to another draft context"
        );
        return (List<E>) draft.resolve();
    }

    private void validateOtherDraft(DraftContext ctx, String errorMessage) {
        if (ctx != null && ctx != this) {
            for (DraftContext parent = this.parent; parent != null; parent = parent.parent) {
                if (parent == ctx) {
                    throw new CircularReferenceException();
                }
            }
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public void addDisposer(Consumer<DraftContext> disposer) {
        if (disposer != null) {
            disposerHolder = new DisposerHolder(disposerHolder, disposer);
        }
    }

    public void dispose() {
        DisposerHolder holder = disposerHolder;
        if (holder != null) {
            holder.dispose(this);
            this.disposerHolder = null;
        }
    }

    private static class DisposerHolder {

        private final DisposerHolder parent;

        private final Consumer<DraftContext> disposer;

        private DisposerHolder(DisposerHolder parent, Consumer<DraftContext> disposer) {
            this.parent = parent;
            this.disposer = disposer;
        }

        public void dispose(DraftContext ctx) {
            disposer.accept(ctx);
            if (parent != null) {
                parent.dispose(ctx);
            }
        }
    }
}
