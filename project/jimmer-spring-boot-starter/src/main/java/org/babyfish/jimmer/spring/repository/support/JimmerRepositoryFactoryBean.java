package org.babyfish.jimmer.spring.repository.support;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class JimmerRepositoryFactoryBean<R extends Repository<E, ID>, E, ID> extends RepositoryFactoryBeanSupport<R, E, ID> {

    private String sqlClientRef;

    public JimmerRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    public void setSqlClientRef(String sqlClientRef) {
        this.sqlClientRef = sqlClientRef;
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        return new JimmerRepositoryFactory();
    }
}
