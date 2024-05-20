package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.DraftInterceptor;

import java.util.*;

abstract class PreHandler {

    private final SaveContext ctx;

    final DraftInterceptor<Object, DraftSpi> draftInterceptor;

    private final Map<PropId, Object> defaultValueMap;

    @SuppressWarnings("unchecked")
    PreHandler(SaveContext ctx) {
        this.ctx = ctx;
        Map<PropId, Object> defaultValueMap = new HashMap<>();
        for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
            Ref<Object> ref = prop.getDefaultValueRef();
            defaultValueMap.put(prop.getId(), ref.getValue());
        }
        this.defaultValueMap = defaultValueMap;
        this.draftInterceptor = (DraftInterceptor<Object, DraftSpi>)
                ctx.options.getSqlClient().getDraftInterceptor(ctx.path.getType());
    }

    abstract void add(DraftSpi draft);

    void applyDefaultValues(DraftSpi spi) {
        for (Map.Entry<PropId, Object> e : defaultValueMap.entrySet()) {
            PropId propId = e.getKey();
            Object value = e.getValue();
            if (!spi.__isLoaded(propId)) {

            }
        }
    }
}

class InsertInterceptorPreHandler extends PreHandler {

    InsertInterceptorPreHandler(SaveContext ctx) {
        super(ctx);
    }

    private ShapedEntityMap<DraftSpi> entityMap = new ShapedEntityMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void add(DraftSpi draft) {

    }
}
