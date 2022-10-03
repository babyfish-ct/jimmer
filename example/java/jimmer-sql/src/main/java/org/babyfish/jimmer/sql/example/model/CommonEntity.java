package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.MappedSuperclass;

import java.time.LocalDateTime;

/*
 * see CommonEntityDraftInterceptor
 */
@MappedSuperclass
public interface CommonEntity {

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();
}
