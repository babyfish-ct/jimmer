package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.MappedSuperclass;

import java.time.LocalDateTime;

@MappedSuperclass
public interface CommonEntity {

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();
}
