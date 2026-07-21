package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

import java.time.LocalDate;

@DatabaseValidationIgnore
@Entity
public interface PgDateTime {

    @Id
    long id();

    @Column(name = "dt")
    LocalDate date();

    @Column(name = "ts")
    java.util.Date dateTime();
}
