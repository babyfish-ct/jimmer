package org.babyfish.jimmer.spring.cfg.support;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.runtime.DefaultTransientResolverProvider;
import org.springframework.context.ApplicationContext;

public final class SpringTransientResolverProvider extends DefaultTransientResolverProvider {

    private final ApplicationContext ctx;

    public SpringTransientResolverProvider(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public TransientResolver<?, ?> get(
            Class<TransientResolver<?, ?>> type,
            JSqlClient sqlClient
    ) throws Exception {
        TransientResolver<?, ?> transientResolver = ctx.getBean(type);
        if (transientResolver != null) {
            return transientResolver;
        }
        return super.get(type, sqlClient);
    }

    @Override
    public TransientResolver<?, ?> get(String ref, JSqlClient sqlClient) throws Exception {
        return (TransientResolver<?, ?>) ctx.getBean(ref);
    }
}
