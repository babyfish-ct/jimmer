package org.babyfish.jimmer.spring.validation;

final class JimmerValidation {

    private static final String JAVAX_CONFIGURATION_CLASS_NAME = "javax.validation.Configuration";

    private static final String JAKARTA_CONFIGURATION_CLASS_NAME = "jakarta.validation.Configuration";

    private JimmerValidation() {
    }

    static void initialize(Object configuration) {
        Class<?> configurationInterface = findConfigurationInterface(configuration.getClass());
        if (configurationInterface == null) {
            throw new IllegalArgumentException(
                    "The configuration object does not implement javax.validation.Configuration " +
                            "or jakarta.validation.Configuration"
            );
        }
        switch (configurationInterface.getName()) {
            case JAVAX_CONFIGURATION_CLASS_NAME:
                JavaxValidation.initialize(configuration);
                break;
            case JAKARTA_CONFIGURATION_CLASS_NAME:
                JakartaValidation.initialize(configuration);
                break;
        }
    }

    private static Class<?> findConfigurationInterface(Class<?> type) {
        for (Class<?> interfaceType : type.getInterfaces()) {
            String interfaceName = interfaceType.getName();
            if (JAVAX_CONFIGURATION_CLASS_NAME.equals(interfaceName) ||
                    JAKARTA_CONFIGURATION_CLASS_NAME.equals(interfaceName)) {
                return interfaceType;
            }
            Class<?> configurationInterface = findConfigurationInterface(interfaceType);
            if (configurationInterface != null) {
                return configurationInterface;
            }
        }
        Class<?> superType = type.getSuperclass();
        return superType != null ? findConfigurationInterface(superType) : null;
    }

    private static final class JavaxValidation {
        static void initialize(Object configuration) {
            javax.validation.Configuration<?> typedConfig = (javax.validation.Configuration<?>) configuration;
            typedConfig.traversableResolver(
                    new JavaxCompositeTraversableResolver(
                            new JimmerJavaxTraversableResolver(),
                            typedConfig.getDefaultTraversableResolver()
                    )
            );
        }
    }

    private static final class JakartaValidation {
        static void initialize(Object configuration) {
            jakarta.validation.Configuration<?> typedConfig = (jakarta.validation.Configuration<?>) configuration;
            typedConfig.traversableResolver(
                    new JakartaCompositeTraversableResolver(
                            new JimmerJakartaTraversableResolver(),
                            typedConfig.getDefaultTraversableResolver()
                    )
            );
        }
    }
}
