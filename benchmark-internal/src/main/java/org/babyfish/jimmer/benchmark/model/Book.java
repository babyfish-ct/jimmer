package org.babyfish.jimmer.benchmark.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

import java.math.BigDecimal;

@Entity
public interface Book {

    @Id
    long id();

    String name();

    int edition();

    BigDecimal price();
}
