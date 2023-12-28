package org.babyfish.jimmer.spring.cfg;

import org.apache.naming.factory.BeanFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public abstract class ClientPathCondition implements Condition {

    ClientPathCondition() {}

    @Override
    public final boolean matches(ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory == null) {
            return false;
        }
        JimmerProperties properties = beanFactory.getBean(JimmerProperties.class);
        return matches(properties.getClient());
    }

    abstract boolean matches(JimmerProperties.Client client);

    public static class TypeScript extends ClientPathCondition {
        @Override
        boolean matches(JimmerProperties.Client client) {
            return client.getTs().getPath() != null;
        }
    }

    public static class OpenApi extends ClientPathCondition {

        @Override
        boolean matches(JimmerProperties.Client client) {
            return client.getOpenapi().getPath() != null;
        }
    }

    public static class OpenApiUI extends ClientPathCondition {

        @Override
        boolean matches(JimmerProperties.Client client) {
            return client.getOpenapi().getUiPath() != null;
        }
    }
}
