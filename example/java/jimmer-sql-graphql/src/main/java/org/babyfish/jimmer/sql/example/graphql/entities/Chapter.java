package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.graphql.entities.common.BaseEntity;
import org.jetbrains.annotations.Nullable;

@Entity
public interface Chapter extends BaseEntity {

    @Id
    long id();

    /*
     * In database, the foreign key `book_id `of `chapter` cannot not null.
     *
     * However, `Chapter.book` is declared as nullable property,
     * because the parent object `Book` may be filtered by global filter
     * (the demo shows how to use global filter).
     */
    @Key
    @Nullable
    @ManyToOne(inputNotNull = true)
    @OnDissociate(DissociateAction.DELETE)
    Book book();

    @Key
    @Column(name = "chapter_no")
    int index();

    String title();
}
