package org.babyfish.jimmer.spring.repository.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class JimmerRepositoryFactoryBean<R extends Repository<E, ID>, E, ID> extends RepositoryFactoryBeanSupport<R, E, ID> implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private String sqlClientRef;

    public JimmerRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
        this.setLazyInit(false);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setSqlClientRef(String sqlClientRef) {
        this.sqlClientRef = sqlClientRef;
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        return new JimmerRepositoryFactory(applicationContext, sqlClientRef);
    }
}
