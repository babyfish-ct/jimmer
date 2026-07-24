package org.babyfish.jimmer.sql.model.inheritance.joinedtable.inverse;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "JOINED_PASSPORT_LINK")
public interface PassportLink {

    @Id
    long id();

    @ManyToOne
    @JoinColumn(name = "PASSPORT_ID")
    Passport passport();
}
