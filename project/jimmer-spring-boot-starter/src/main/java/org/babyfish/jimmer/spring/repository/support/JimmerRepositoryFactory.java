package org.babyfish.jimmer.spring.repository.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JimmerRepositoryFactory extends RepositoryFactorySupport {

    @NotNull
    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return null;
    }

    @NotNull
    @Override
    protected Object getTargetRepository(RepositoryInformation metadata) {
        Class<?> clazz = metadata.getRepositoryInterface();
        return Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[]{clazz},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        throw new UnsupportedOperationException("The method " + method + " is not supported");
                    }
                }
        );
    }

    @NotNull
    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return metadata.getRepositoryInterface();
    }
}
