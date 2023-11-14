package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.AuthorTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface AuthorRepository extends JRepository<Author, Long> {

    AuthorTable table = AuthorTable.$;

    default List<Author> findByName(String name) {
        return sql()
                .createQuery(table)
                .whereIf(
                        name != null,
                        () -> Predicate.or(
                                table.firstName().eq(name),
                                table.lastName().eq(name)
                        )
                )
                .select(table)
                .execute();
    }
}
