package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorTable;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

public interface AuthorRepository extends JRepository<Author, Long> {

    AuthorTable table = AuthorTable.$;

    default List<Author> findByName(@Nullable String name) {

        return sql()
                .createQuery(table)
                .whereIf(
                        StringUtils.hasText(name),
                        Predicate.or(
                                table.firstName().ilike(name),
                                table.lastName().ilike(name)
                        )
                )
                .orderBy(table.firstName(), table.lastName())
                .select(table)
                .execute();
    }
}
