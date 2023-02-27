package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.runtime.TransientResolverProvider;
import org.springframework.context.ApplicationContext;

public final class SpringTransientResolverProvider implements TransientResolverProvider {

    private final ApplicationContext ctx;

    public SpringTransientResolverProvider(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public TransientResolver<?, ?> get(
            Class<TransientResolver<?, ?>> resolverType,
            JSqlClient sqlClient
    ) throws Exception {
        return ctx.getBean(resolverType);
    }

    @Override
    public TransientResolver<?, ?> get(String ref) throws Exception {
        return (TransientResolver<?, ?>) ctx.getBean(ref);
    }
}
