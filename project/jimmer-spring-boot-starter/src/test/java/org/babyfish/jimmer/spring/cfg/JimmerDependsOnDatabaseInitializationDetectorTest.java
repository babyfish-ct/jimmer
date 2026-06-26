package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JimmerDependsOnDatabaseInitializationDetectorTest {

    @Test
    public void testDetectSqlClientBeans() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition("javaSqlClient", new RootBeanDefinition(JSqlClient.class));
        beanFactory.registerBeanDefinition("kotlinSqlClient", new RootBeanDefinition(KSqlClient.class));
        beanFactory.registerBeanDefinition("otherBean", new RootBeanDefinition(String.class));

        Set<String> names = new JimmerDependsOnDatabaseInitializationDetector().detect(beanFactory);

        Assertions.assertEquals(
                new HashSet<>(Arrays.asList("javaSqlClient", "kotlinSqlClient")),
                names
        );
    }

    @Test
    public void testSpringFactoriesRegistration() {
        Assertions.assertTrue(
                SpringFactoriesLoader
                        .loadFactoryNames(
                                DependsOnDatabaseInitializationDetector.class,
                                getClass().getClassLoader()
                        )
                        .contains(JimmerDependsOnDatabaseInitializationDetector.class.getName())
        );
    }
}
