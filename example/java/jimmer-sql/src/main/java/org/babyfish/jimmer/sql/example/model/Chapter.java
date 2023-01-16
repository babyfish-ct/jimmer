package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;

@Entity
public interface Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @ManyToOne
    @Key
    @OnDissociate(DissociateAction.DELETE)
    Book book();

    @Key
    int index();

    String title();
}
