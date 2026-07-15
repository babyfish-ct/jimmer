package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

@Configuration
@ConditionalOnClass(name = "org.springframework.validation.beanvalidation.LocalValidatorFactoryBean")
public class JimmerBeanValidationConfig {

    @Bean
    public BeanPostProcessor jimmerValidationBeanPostProcessor() {
        return new JimmerValidationBeanPostProcessor();
    }

    private static class JimmerValidationBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (!isLocalValidatorFactoryBean(bean)) {
                return bean;
            }
            configure(bean);
            return bean;
        }

        @SuppressWarnings("unchecked")
        private static void configure(Object bean) {
            Method setter = ReflectionUtils.findMethod(bean.getClass(), "setConfigurationInitializer", Consumer.class);
            if (setter == null) {
                return;
            }
            Field field = ReflectionUtils.findField(bean.getClass(), "configurationInitializer");
            ReflectionUtils.makeAccessible(setter);
            Consumer<Object> jimmerInitializer = JimmerValidationBeanPostProcessor::configureValidation;
            if (field != null) {
                ReflectionUtils.makeAccessible(field);
                Consumer<Object> oldInitializer = (Consumer<Object>) ReflectionUtils.getField(field, bean);
                if (oldInitializer != null) {
                    jimmerInitializer = oldInitializer.andThen(jimmerInitializer);
                }
            }
            ReflectionUtils.invokeMethod(setter, bean, jimmerInitializer);
        }

        private static void configureValidation(Object configuration) {
            try {
                Class<?> traversableResolverType = traversableResolverType(configuration.getClass().getClassLoader());
                Method getDefaultTraversableResolver = configuration.getClass().getMethod("getDefaultTraversableResolver");
                Object defaultTraversableResolver = getDefaultTraversableResolver.invoke(configuration);
                Object jimmerTraversableResolver = Proxy.newProxyInstance(
                        traversableResolverType.getClassLoader(),
                        new Class<?>[] { traversableResolverType },
                        (proxy, method, args) -> {
                            String methodName = method.getName();
                            if (("isReachable".equals(methodName) || "isCascadable".equals(methodName)) &&
                                    args != null &&
                                    args.length == 5 &&
                                    !isLoaded(args[0], args[1])) {
                                return false;
                            }
                            return method.invoke(defaultTraversableResolver, args);
                        }
                );
                Method traversableResolver = configuration.getClass().getMethod(
                        "traversableResolver",
                        traversableResolverType
                );
                traversableResolver.invoke(configuration, jimmerTraversableResolver);
            } catch (ClassNotFoundException ex) {
                // Bean Validation is unavailable, nothing to customize.
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Cannot configure Bean Validation for Jimmer immutable objects", ex);
            }
        }

        private static Class<?> traversableResolverType(ClassLoader classLoader) throws ClassNotFoundException {
            try {
                return Class.forName("javax.validation.TraversableResolver", false, classLoader);
            } catch (ClassNotFoundException ex) {
                return Class.forName("jakarta.validation.TraversableResolver", false, classLoader);
            }
        }

        private static boolean isLoaded(Object traversableObject, Object traversableProperty) throws ReflectiveOperationException {
            if (!(traversableObject instanceof ImmutableSpi) || traversableProperty == null) {
                return true;
            }
            Method getName = traversableProperty.getClass().getMethod("getName");
            String propName = (String) getName.invoke(traversableProperty);
            return propName == null || ((ImmutableSpi) traversableObject).__isLoaded(propName);
        }

        private static boolean isLocalValidatorFactoryBean(Object bean) {
            Class<?> type = bean.getClass();
            while (type != null) {
                if ("org.springframework.validation.beanvalidation.LocalValidatorFactoryBean".equals(type.getName())) {
                    return true;
                }
                type = type.getSuperclass();
            }
            return false;
        }
    }
}
