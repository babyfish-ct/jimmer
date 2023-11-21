package org.babyfish.jimmer.spring.cfg.support;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.di.LogicalDeletedValueGeneratorProvider;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.springframework.context.ApplicationContext;

public class SpringLogicalDeletedValueGeneratorProvider implements LogicalDeletedValueGeneratorProvider {

    private final ApplicationContext ctx;

    public SpringLogicalDeletedValueGeneratorProvider(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public LogicalDeletedValueGenerator<?> get(String ref, JSqlClient sqlClient) throws Exception {
        Object bean = ctx.getBean(ref);
        if (!(bean instanceof LogicalDeletedValueGenerator<?>)) {
            throw new IllegalStateException(
                    "The expected type of spring bean named \"ref\" is \"" +
                            LogicalDeletedValueGenerator.class.getName() +
                            "\", but the actual type is + \"" +
                            bean.getClass().getName() +
                            "\""
            );
        }
        return (LogicalDeletedValueGenerator<?>) bean;
    }
}
