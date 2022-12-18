package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.KRepository;
import org.babyfish.jimmer.spring.repository.bytecode.ClassCodeWriter;
import org.babyfish.jimmer.spring.repository.bytecode.JavaClassCodeWriter;
import org.babyfish.jimmer.spring.repository.bytecode.JavaClasses;
import org.babyfish.jimmer.spring.repository.bytecode.KotlinClassCodeWriter;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import java.lang.reflect.InvocationTargetException;

public class JimmerRepositoryFactory extends RepositoryFactorySupport {

    private final Object sqlClient;

    public JimmerRepositoryFactory(Object sqlClient) {
        this.sqlClient = sqlClient;
    }

    @NotNull
    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return null;
    }

    @NotNull
    @Override
    protected Object getTargetRepository(RepositoryInformation metadata) {
        Class<?> repositoryInterface = metadata.getRepositoryInterface();
        boolean jRepository = JRepository.class.isAssignableFrom(repositoryInterface);
        boolean kRepository = KRepository.class.isAssignableFrom(repositoryInterface);
        if (jRepository && kRepository) {
            throw new IllegalStateException(
                    "Illegal repository interface \"" +
                            repositoryInterface.getName() +
                            "\", it can not extend both \"" +
                            JRepository.class.getName() +
                            "\" and \"" +
                            KRepository.class.getName() +
                            "\""
            );
        }
        if (sqlClient instanceof JSqlClient) {
            if (!jRepository) {
                throw new IllegalStateException(
                        "The type of current sqlClient object is \"" +
                                JSqlClient.class.getName() +
                                "\", but repository interface \"" +
                                repositoryInterface.getName() +
                                "\" does not extend  \"" +
                                JRepository.class.getName() +
                                "\""
                );
            }
        } else if (sqlClient instanceof KSqlClient) {
            if (!kRepository) {
                throw new IllegalStateException(
                        "The type of current sqlClient object is \"" +
                                KSqlClient.class.getName() +
                                "\", but repository interface \"" +
                                repositoryInterface.getName() +
                                "\" does not extend  \"" +
                                KRepository.class.getName() +
                                "\""
                );
            }
        } else {
            throw new IllegalStateException(
                    "Illegal repository interface \"" +
                            repositoryInterface.getName() +
                            "\", it is neither \"" +
                            JRepository.class.getName() +
                            "\" nor \"" +
                            KRepository.class.getName() +
                            "\""
            );
        }
        Class<?> clazz = null;
        try {
            clazz = Class.forName(
                    ClassCodeWriter.implementationClassName(repositoryInterface),
                    true,
                    repositoryInterface.getClassLoader()
            );
        } catch (ClassNotFoundException ex) {
            // Do nothing
        }
        if (clazz == null) {
            ClassCodeWriter writer = jRepository ?
                    new JavaClassCodeWriter(metadata) :
                    new KotlinClassCodeWriter(metadata);
            byte[] bytecode = writer.write();
            clazz = JavaClasses.define(bytecode, repositoryInterface);
        }
        try {
            return clazz.getConstructor(jRepository ? JSqlClient.class : KSqlClient.class).newInstance(sqlClient);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
            throw new AssertionError("Internal bug", ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("Internal bug", ex.getTargetException());
        }
    }

    @NotNull
    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return metadata.getRepositoryInterface();
    }
}
