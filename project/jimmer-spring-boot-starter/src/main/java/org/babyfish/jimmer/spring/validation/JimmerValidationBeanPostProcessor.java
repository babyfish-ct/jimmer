package org.babyfish.jimmer.spring.validation;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class JimmerValidationBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (bean instanceof LocalValidatorFactoryBean) {
            configure((LocalValidatorFactoryBean) bean);
        }
        return bean;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void configure(LocalValidatorFactoryBean bean) {
        if (fieldValue(bean, "traversableResolver") != null) {
            return;
        }
        Consumer<Object> jimmerInitializer = JimmerValidation::initialize;
        Consumer<Object> userInitializer = (Consumer<Object>) fieldValue(bean, "configurationInitializer");
        if (userInitializer != null) {
            jimmerInitializer = jimmerInitializer.andThen(userInitializer);
        }
        // The erased setter is identical in Spring 5 and 6, but its generic
        // Configuration type is javax.validation or jakarta.validation.
        bean.setConfigurationInitializer((Consumer) jimmerInitializer);
    }

    private static Object fieldValue(LocalValidatorFactoryBean bean, String name) {
        try {
            Field field = LocalValidatorFactoryBean.class.getDeclaredField(name);
            ReflectionUtils.makeAccessible(field);
            return ReflectionUtils.getField(field, bean);
        } catch (NoSuchFieldException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
