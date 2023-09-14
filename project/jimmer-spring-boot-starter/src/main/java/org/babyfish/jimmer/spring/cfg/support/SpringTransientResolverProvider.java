package org.babyfish.jimmer.spring.cfg.support;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.runtime.DefaultTransientResolverProvider;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public class SpringTransientResolverProvider extends DefaultTransientResolverProvider {

    private final ApplicationContext ctx;

    public SpringTransientResolverProvider(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public TransientResolver<?, ?> get(
            Class<TransientResolver<?, ?>> type,
            JSqlClient sqlClient
    ) throws Exception {
        Map<String, TransientResolver<?, ?>> map = ctx.getBeansOfType(type);
        if (map.isEmpty()) {
            return super.get(type, sqlClient);
        }
        if (map.size() > 1) {
            throw new IllegalStateException("Two many spring beans whose type is \"" + type.getName() + "\"");
        }
        return map.values().iterator().next();
    }

    @Override
    public TransientResolver<?, ?> get(String ref, JSqlClient sqlClient) throws Exception {
        Object bean = ctx.getBean(ref);
        if (!(bean instanceof TransientResolver<?, ?>)) {
            throw new IllegalStateException(
                    "The expected type of spring bean named \"ref\" is \"" +
                            TransientResolver.class.getName() +
                            "\", but the actual type is + \"" +
                            bean.getClass().getName() +
                            "\""
            );
        }
        return (TransientResolver<?, ?>) bean;
    }

    @Override
    public final boolean shouldResolversBeCreatedImmediately() {
        return false;
    }
}
