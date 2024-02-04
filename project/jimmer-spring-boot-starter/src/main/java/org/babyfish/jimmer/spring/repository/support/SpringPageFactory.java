package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableRootQuerySource;
import org.babyfish.jimmer.sql.ast.query.PageFactory;
import org.springframework.data.domain.Page;

import java.util.List;

public class SpringPageFactory<E> implements PageFactory<E, Page<E>> {

    private SpringPageFactory() {}

    @Override
    public Page<E> create(List<E> entities, long totalCount, ConfigurableRootQuerySource source) {
        return null;
    }

    static <E> SpringPageFactory<E> create() {
        return new SpringPageFactory<>();
    }
}
