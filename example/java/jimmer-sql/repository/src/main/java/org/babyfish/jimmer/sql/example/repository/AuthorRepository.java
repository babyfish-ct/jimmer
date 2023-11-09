package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.AuthorTable;
import org.babyfish.jimmer.sql.example.repository.dto.AuthorSpecification;
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
        return sql().createQuery(table)
                .where(specification) // ❸
                .orderBy(SpringOrders.toOrders(table, sort)) // ❻
                .select(table.fetch(fetcher)) // ❼
                .execute();
    }
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/spring/repository/concept
❷ https://babyfish-ct.github.io/jimmer/docs/spring/repository/default
❸ https://babyfish-ct.github.io/jimmer/docs/query/qbe
❻ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-order
❼ https://babyfish-ct.github.io/jimmer/docs/query/object-fetcher/
---------------------------------------------------*/
