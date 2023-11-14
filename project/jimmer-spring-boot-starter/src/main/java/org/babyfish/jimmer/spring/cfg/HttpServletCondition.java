package org.babyfish.jimmer.spring.cfg;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class HttpServletCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            Class.forName("javax.servlet.http.HttpServlet");
            return true;
        } catch (ClassNotFoundException e) {
            // Do nothing
        }
        try {
            Class.forName("jakarta.servlet.http.HttpServlet");
            return true;
        } catch (ClassNotFoundException e) {
            // Do nothing
        }
        return false;
    }
}
