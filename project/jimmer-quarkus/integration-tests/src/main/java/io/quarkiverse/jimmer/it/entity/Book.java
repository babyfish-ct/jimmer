package io.quarkiverse.jimmer.it.entity;

import java.math.BigDecimal;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id();

    String name();

    int edition();

    BigDecimal price();

    int storeId();
}
