package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.sql.ast.impl.query.PageSource;
import org.babyfish.jimmer.sql.ast.query.PageFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public class SpringPageFactory<E> implements PageFactory<E, Page<E>> {

    private static final SpringPageFactory<?> INSTANCE = new SpringPageFactory<>();

    private SpringPageFactory() {}

    @Override
    public Page<E> create(List<E> entities, long totalCount, PageSource source) {
        return new PageImpl<>(
                entities,
                PageRequest.of(
                        source.getPageIndex(),
                        source.getPageSize(),
                        Utils.toSort(
                                source.getOrders(),
                                source.getSqlClient().getMetadataStrategy()
                        )
                ),
                totalCount
        );
    }

    @SuppressWarnings("unchecked")
    public static <E> SpringPageFactory<E> getInstance() {
        return (SpringPageFactory<E>) INSTANCE;
    }
}
