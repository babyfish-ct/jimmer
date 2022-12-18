package org.babyfish.jimmer.spring.repository.config;

import org.babyfish.jimmer.spring.repository.support.JimmerRepositoryFactoryBean;
import org.babyfish.jimmer.sql.Entity;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class JimmerRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

    @NotNull
    @Override
    public String getModuleName() {
        return "Jimmer";
    }

    @NotNull
    @Override
    public String getRepositoryFactoryBeanClassName() {
        return JimmerRepositoryFactoryBean.class.getName();
    }

    @NotNull
    @Override
    protected String getModulePrefix() {
        return getModuleName().toLowerCase(Locale.US);
    }

    @NotNull
    @Override
    public void postProcess(@NotNull BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
        source.getAttribute("sqlClientRef") //
        .filter(StringUtils::hasText) //
        .ifPresent(s -> builder.addPropertyReference("sqlClient", s));
    }

    @NotNull
    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Collections.singleton(Entity.class);
    }
}
