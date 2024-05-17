package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.IdentityHashMap;

abstract class PreHandler {

    private static final DraftInterceptor<Object, DraftSpi> NIL_INTERCEPTOR =
            (draft, original) -> {
                throw new UnsupportedOperationException();
            };

    private final SaveContext ctx;

    private DraftInterceptor<Object, DraftSpi> interceptor;

    PreHandler(SaveContext ctx) {
        this.ctx = ctx;
    }

    abstract void add(DraftSpi draft);

    @SuppressWarnings("unchecked")
    protected final DraftInterceptor<Object, DraftSpi> interceptor() {
        DraftInterceptor<Object, DraftSpi> interceptor = this.interceptor;
        if (interceptor == null) {
            interceptor = (DraftInterceptor<Object, DraftSpi>)
                    ctx.options.getSqlClient().getDraftInterceptor(ctx.path.getType());
            if (interceptor == null) {
                interceptor = NIL_INTERCEPTOR;
            }
            this.interceptor = interceptor;
        }
        return interceptor == NIL_INTERCEPTOR ? null : interceptor;
    }
}

class InsertPreHandler extends PreHandler {

    InsertPreHandler(SaveContext ctx) {
        super(ctx);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(DraftSpi draft) {
        DraftInterceptor<Object, DraftSpi> interceptor = interceptor();
        interceptor.beforeSave(draft, null);
    }
}

abstract class AbstractUpdateablePreHandler extends PreHandler {

    AbstractUpdateablePreHandler(SaveContext ctx) {
        super(ctx);
    }

    @Override
    void add(DraftSpi draft) {

    }
}
