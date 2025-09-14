package org.babyfish.jimmer.spring.java.model.issue1162;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Entity
public interface Organization {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    @NotNull
    UUID id();

    @Key
    @Nullable
    UUID idmOrgId();
}

