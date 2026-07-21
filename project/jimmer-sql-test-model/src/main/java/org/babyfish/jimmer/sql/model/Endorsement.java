package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Minimal reproduction model for the trigger FK-key-unload bug:
 * a join-like entity with two real-FK `@Key` props referencing two
 * distinct parent entity types, plus a nullable `@Key` string,
 * and its own client-side-generated id (loaded on the draft before insert).
 */
@DatabaseValidationIgnore
@Entity
@KeyUniqueConstraint(noMoreUniqueConstraints = true, isNullNotDistinct = true)
public interface Endorsement {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    @Nullable
    @Key
    String code();

    @Key
    @ManyToOne
    @OnDissociate(DissociateAction.LAX)
    BookStore bookStore();

    @Key
    @ManyToOne
    @OnDissociate(DissociateAction.LAX)
    Author author();

    String level();
}
