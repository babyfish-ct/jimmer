package org.babyfish.jimmer.spring.cfg.support;

import org.babyfish.jimmer.spring.util.ApplicationContextUtils;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.di.DefaultUserIdGeneratorProvider;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

public class SpringUserIdGeneratorProvider extends DefaultUserIdGeneratorProvider {

    private final ApplicationContext ctx;

    public SpringUserIdGeneratorProvider(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public UserIdGenerator<?> get(Class<UserIdGenerator<?>> type, JSqlClient sqlClient) throws Exception {
        List<UserIdGenerator<?>> userIdGenerators = ApplicationContextUtils.getBeansOfType(ctx, type);
        if (userIdGenerators.isEmpty()) {
            return super.get(type, sqlClient);
        }
        if (userIdGenerators.size() > 1) {
            throw new IllegalStateException("Two many spring beans whose type is \"" + type.getName() + "\"");
        }
        return userIdGenerators.get(0);
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
