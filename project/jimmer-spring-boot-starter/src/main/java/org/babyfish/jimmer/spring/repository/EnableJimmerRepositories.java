package org.babyfish.jimmer.spring.repository;

import kotlin.Deprecated;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoriesRegistrar;
import org.babyfish.jimmer.spring.repository.support.JimmerRepositoryFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(JimmerRepositoriesRegistrar.class)
public @interface EnableJimmerRepositories {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
     * {@code @EnableJimmerRepositories("org.my.pkg")} instead of
     * {@code @EnableJimmerRepositories(basePackages="org.my.pkg")}.
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
     * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
     */
    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
     * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
     * each package that serves no purpose other than being referenced by this attribute.
     */
    Class<?>[] basePackageClasses() default {};

    ComponentScan.Filter[] includeFilters() default {};

    /**
     * Specifies which types are not eligible for component scanning.
     */
    ComponentScan.Filter[] excludeFilters() default {};

    /**
     * Returns the {@link FactoryBean} class to be used for each repository instance. Defaults to
     * {@link org.babyfish.jimmer.spring.repository.support.JimmerRepositoryFactoryBean}.
     */
    Class<?> repositoryFactoryBeanClass() default JimmerRepositoryFactoryBean.class;

    /**
     * Configures the name of the JSqlClient/KSqlClient bean definition to be
     * used to create repositories discovered through this annotation.
     * Defaults to sqlClient.
     */
    String sqlClientRef() default "sqlClient";

    @Deprecated(message = "Jimmer does not need it, but spring data extension requires it")
    String namedQueriesLocation() default "";

    @Deprecated(message = "Jimmer does not need it, but spring data extension requires it")
    String repositoryImplementationPostfix() default "Impl";
}
