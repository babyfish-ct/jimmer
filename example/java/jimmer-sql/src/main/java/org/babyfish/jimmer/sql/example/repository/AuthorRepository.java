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

public interface AuthorRepository extends JRepository<Author, Long> {

    AuthorTable table = AuthorTable.$;

    default List<Author> find(
            AuthorSpecification specification,
            Sort sort,
            Fetcher<Author> fetcher
    ) {
        Example<Author> example = Example
                .of(specification.toEntity())
                .ilike(AuthorProps.FIRST_NAME)
                .ilike(AuthorProps.LAST_NAME)
                .trim();

        return sql().createQuery(table)
                .where(table.eq(example))
                .whereIf(
                        specification.getMinCreatedTime() != null,
                        () -> table.createdTime().ge(specification.getMinCreatedTime())
                )
                .whereIf(
                        specification.getMaxCreatedTimeExclusive() != null,
                        () -> table.createdTime().lt(specification.getMaxCreatedTimeExclusive())
                )
                .orderBy(SpringOrders.toOrders(table, sort))
                .select(table.fetch(fetcher))
                .execute();
    }
}
