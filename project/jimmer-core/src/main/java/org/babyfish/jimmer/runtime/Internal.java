package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.DraftConsumer;
import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.meata.ImmutableType;

public class Internal {

    private static final ThreadLocal<DraftContext> DRAFT_CONTEXT_LOCAL =
            new ThreadLocal<>();

    private Internal() {}

    public static Object produce(
            ImmutableType type,
            Object base,
            DraftConsumer<? extends Draft> block
    ) {
        DraftContext ctx = DRAFT_CONTEXT_LOCAL.get();
        if (ctx != null) {
            return createDraft(ctx, type, base, block);
        } else {
            ctx = new DraftContext();
            DRAFT_CONTEXT_LOCAL.set(ctx);
            try {
                Draft draft = createDraft(ctx, type, base, block);
                return ctx.resolveObject(draft);
            } finally {
                DRAFT_CONTEXT_LOCAL.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Draft createDraft(
            DraftContext ctx,
            ImmutableType type,
            Object base,
            DraftConsumer<? extends Draft> block) {
        Draft draft = type.getDraftFactory().apply(ctx, base);
        if (block != null) {
            try {
                ((DraftConsumer<Draft>) block).accept(draft);
            } catch (Throwable ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException)ex;
                }
                if (ex instanceof Error) {
                    throw (Error)ex;
                }
                throw new DraftConsumerUncheckedException(ex);
            }
        }
        return draft;
    }
}
