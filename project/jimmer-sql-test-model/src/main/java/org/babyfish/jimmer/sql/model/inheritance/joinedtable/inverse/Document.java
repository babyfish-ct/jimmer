package org.babyfish.jimmer.sql.model.inheritance.joinedtable.inverse;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "JOINED_DOCUMENT")
@Inheritance(strategy = InheritanceType.JOINED)
public interface Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    @Column(name = "DOCUMENT_TYPE")
    String type();

    String name();
}
