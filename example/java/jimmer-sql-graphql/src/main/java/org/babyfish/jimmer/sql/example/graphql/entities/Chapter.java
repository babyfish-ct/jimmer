package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.graphql.entities.common.BaseEntity;

import javax.validation.constraints.Null;

@Entity
@StaticType(alias = "forBookInput", autoScalarStrategy = AutoScalarStrategy.DECLARED)
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
    @Null
    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Book book();

    @Key
    @Column(name = "chapter_no")
    int index();

    String title();
}
