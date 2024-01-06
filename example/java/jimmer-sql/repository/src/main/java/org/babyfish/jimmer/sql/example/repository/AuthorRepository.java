package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.AuthorTable;
import org.babyfish.jimmer.sql.example.model.Tables;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface AuthorRepository extends JRepository<Author, Long>, Tables {

    AuthorTable table = AUTHOR_TABLE;

    default List<Author> find(
            JSpecification<?, AuthorTable> specification,
            Sort sort,
            Fetcher<Author> fetcher
    ) {
        return sql().createQuery(table)
                .where(specification)
                .orderBy(SpringOrders.toOrders(table, sort))
                .select(table.fetch(fetcher))
                .execute();
    }
}
