package org.babyfish.jimmer.spring.cfg.support;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.runtime.DefaultUserIdGeneratorProvider;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public class SpringUserIdGeneratorProvider extends DefaultUserIdGeneratorProvider {

    private final ApplicationContext ctx;

    public SpringUserIdGeneratorProvider(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public UserIdGenerator<?> get(Class<UserIdGenerator<?>> type, JSqlClient sqlClient) throws Exception {
        Map<String, UserIdGenerator<?>> map = ctx.getBeansOfType(type);
        if (map.isEmpty()) {
            return super.get(type, sqlClient);
        }
        if (map.size() > 1) {
            throw new IllegalStateException("Two many spring beans whose type is \"" + type.getName() + "\"");
        }
        return map.values().iterator().next();
    }

    @Override
    public UserIdGenerator<?> get(String ref, JSqlClient sqlClient) throws Exception {
        Object bean = ctx.getBean(ref);
        if (!(bean instanceof UserIdGenerator<?>)) {
            throw new IllegalStateException(
                    "The expected type of spring bean named \"ref\" is \"" +
                            UserIdGenerator.class.getName() +
                            "\", but the actual type is + \"" +
                            bean.getClass().getName() +
                            "\""
            );
        }
        return (UserIdGenerator<?>) bean;
    }
}
