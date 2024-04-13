package org.babyfish.jimmer.spring.util;

import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link org.springframework.context.ApplicationContext#getBeansOfType(Class)}
 * hides exceptions such as dependency cycle. Use this class to replace it.
 */
public class ApplicationContextUtils {

    @SuppressWarnings("unchecked")
    public static <T> List<T> getBeansOfType(ApplicationContext ctx, Class<T> type) {
        String[] beanNames = ctx.getBeanNamesForType(type);
        if (beanNames.length == 0) {
            return Collections.emptyList();
        }
        List<T> beans = new ArrayList<>(beanNames.length);
        for (String beanName : beanNames) {
            Object bean = ctx.getBean(beanName);
            if (!bean.getClass().getName().equals("org.springframework.beans.factory.support.NullBean")) {
                beans.add((T)bean);
            }
        }
        return beans;
    }
}
