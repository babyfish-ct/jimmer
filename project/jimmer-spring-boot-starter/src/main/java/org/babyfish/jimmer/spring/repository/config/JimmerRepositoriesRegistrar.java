package org.babyfish.jimmer.spring.repository.config;

import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

public class JimmerRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableJimmerRepositories.class;
    }

    @Override
    protected Class<?> getConfiguration() {
        return JimmerRepositoriesRegistrar.EnableJimmerRepositoriesConfiguration.class;
    }

    @Override
    protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
        return new JimmerRepositoryConfigExtension();
    }

    @EnableJimmerRepositories
    private static class EnableJimmerRepositoriesConfiguration {}
}
