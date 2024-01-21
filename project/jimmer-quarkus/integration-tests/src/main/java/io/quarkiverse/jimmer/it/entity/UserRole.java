package io.quarkiverse.jimmer.it.entity;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;

import io.quarkiverse.jimmer.it.config.StringIdGenerator;

@Entity
public interface UserRole {

    @Id
    @GeneratedValue(generatorType = StringIdGenerator.class)
    String id();

    String userId();

    String roleId();

    boolean deleteFlag();
}
