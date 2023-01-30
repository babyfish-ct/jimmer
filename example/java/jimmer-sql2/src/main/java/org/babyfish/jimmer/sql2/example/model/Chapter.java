package org.babyfish.jimmer.sql2.example.model;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql2.example.model.common.BaseEntity;
import org.jetbrains.annotations.Nullable;

@Entity
public interface Chapter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Nullable
    @ManyToOne(inputNotNull = true)
    @Key
    @OnDissociate(DissociateAction.DELETE)
    Book book();

    @Key
    @Column(name = "chapter_no")
    int index();

    String title();
}
