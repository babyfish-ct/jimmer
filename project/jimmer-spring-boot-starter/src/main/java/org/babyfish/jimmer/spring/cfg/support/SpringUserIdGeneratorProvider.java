package org.babyfish.jimmer.spring.cfg.support;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.runtime.DefaultUserIdGeneratorProvider;
import org.springframework.context.ApplicationContext;

public class SpringUserIdGeneratorProvider extends DefaultUserIdGeneratorProvider {

    private final ApplicationContext applicationContext;

    public SpringUserIdGeneratorProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public UserIdGenerator<?> get(Class<UserIdGenerator<?>> type, JSqlClient sqlClient) throws Exception {
        UserIdGenerator<?> userIdGenerator = applicationContext.getBean(type);
        if (userIdGenerator != null) {
            return userIdGenerator;
        }
        return super.get(type, sqlClient);
    }
}
