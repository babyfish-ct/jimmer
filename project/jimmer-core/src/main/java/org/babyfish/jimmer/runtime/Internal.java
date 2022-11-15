package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.DraftConsumer;
import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Internal {

    private static final ThreadLocal<DraftContext> DRAFT_CONTEXT_LOCAL =
            new ThreadLocal<>();

    private Internal() {}

    public static Object produce(
            ImmutableType type,
            Object base,
            DraftConsumer<?> block
    ) {
        return usingDraftContext((ctx, isRoot) -> {
            Object draft = createDraft(ctx, type, base);
            modifyDraft(draft, block);
            return isRoot ? ctx.resolveObject(draft) : draft;
        });
    }

    public static Object produce(
            ImmutableType type,
            Object base,
            DraftConsumer<?> block,
            Consumer<DraftContext> disposer
    ) {
        return usingDraftContext((ctx, isRoot) -> {
            ctx.addDisposer(disposer);
            Object draft = createDraft(ctx, type, base);
            modifyDraft(draft, block);
            return isRoot ? ctx.resolveObject(draft) : draft;
        });
    }

    public static List<Object> produceList(
            ImmutableType type,
            Collection<?> bases,
            DraftConsumer<List<? extends Draft>> block
    ) {
        if (bases.isEmpty()) {
            return Collections.emptyList();
        }
        return usingDraftContext((ctx, isRoot) -> {
            Object[] arr = new Object[bases.size()];
            int index = 0;
            for (Object base : bases) {
                if (base != null) {
                    arr[index] = createDraft(ctx, type, base);
                }
                index++;
            }
            modifyDraft(Arrays.asList(arr), block);
            if (isRoot) {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = ctx.resolveObject(arr[i]);
                }
            }
            return Collections.unmodifiableList(Arrays.asList(arr));
        });
    }

    public static DraftContext currentDraftContext() {
        DraftContext ctx = DRAFT_CONTEXT_LOCAL.get();
        if (ctx == null) {
            throw new IllegalStateException("No draft context");
        }
        return ctx;
    }

    private static <T> T usingDraftContext(
            BiFunction<DraftContext, Boolean, T> block
    ) {
        DraftContext ctx = DRAFT_CONTEXT_LOCAL.get();
        if (ctx != null) {
            return block.apply(ctx, false);
        }
        ctx = new DraftContext(null);
        DRAFT_CONTEXT_LOCAL.set(ctx);
        try {
            T result = block.apply(ctx, true);
            ctx.dispose();
            return result;
        } finally {
            DRAFT_CONTEXT_LOCAL.remove();
        }
    }

    public static <T> T requiresNewDraftContext(
            Function<DraftContext, T> block
    ) {
        DraftContext oldCtx = DRAFT_CONTEXT_LOCAL.get();
        DraftContext ctx = new DraftContext(oldCtx);
        DRAFT_CONTEXT_LOCAL.set(ctx);
        try {
            return block.apply(ctx);
        } finally {
            if (oldCtx != null) {
                DRAFT_CONTEXT_LOCAL.set(oldCtx);
            } else {
                DRAFT_CONTEXT_LOCAL.remove();
            }
        }
    }

    private static Draft createDraft(
            DraftContext ctx,
            ImmutableType type,
            Object base) {
        Draft draft;
        if (base instanceof Draft) {
            if (((DraftSpi)base).__draftContext() != ctx) {
                throw new IllegalArgumentException("base cannot be draft of another draft context");
            }
            draft = (Draft) base;
        } else {
            draft = type.getDraftFactory().apply(ctx, base);
        };
        return draft;
    }

    @SuppressWarnings("unchecked")
    private static void modifyDraft(
            Object draft,
            DraftConsumer<?> block
    ) {
        if (block != null) {
            try {
                ((DraftConsumer<Object>) block).accept(draft);
            } catch (Throwable ex) {
                DraftConsumerUncheckedException.rethrow(ex);
            }
        }
    }
}