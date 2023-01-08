package org.babyfish.jimmer.spring.cfg;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class MetadataCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty("jimmer.client.ts.path")) ||
                StringUtils.hasText(context.getEnvironment().getProperty("jimmer.client.java-feign.path"));
    }
}
