package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.AuthorProps;
import org.babyfish.jimmer.sql.example.model.AuthorTable;
import org.babyfish.jimmer.sql.example.model.dto.AuthorSpecification;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface AuthorRepository extends JRepository<Author, Long> { // ❶

    AuthorTable table = AuthorTable.$;

    default List<Author> find( // ❷
            AuthorSpecification specification,
            Sort sort,
            Fetcher<Author> fetcher
    ) {
        Example<Author> example = Example // ❶
                .of(specification.toEntity())
                .ilike(AuthorProps.FIRST_NAME)
                .ilike(AuthorProps.LAST_NAME)
                .trim();

        return sql().createQuery(table)
                .where(table.eq(example)) // ❸
                .whereIf( // ❹
                        specification.getMinCreatedTime() != null,
                        () -> table.createdTime().ge(specification.getMinCreatedTime())
                )
                .whereIf( // ❺
                        specification.getMaxCreatedTimeExclusive() != null,
                        () -> table.createdTime().lt(specification.getMaxCreatedTimeExclusive())
                )
                .orderBy(SpringOrders.toOrders(table, sort)) // ❻
                .select(table.fetch(fetcher)) // ❼
                .execute();
    }
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/spring/repository/concept
❷ https://babyfish-ct.github.io/jimmer/docs/spring/repository/default
❸ https://babyfish-ct.github.io/jimmer/docs/query/qbe
❹ ❺ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-where
❻ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-order
❼ https://babyfish-ct.github.io/jimmer/docs/query/object-fetcher/
---------------------------------------------------*/
