package org.babyfish.jimmer.sql.example.model.common;

import org.babyfish.jimmer.sql.MappedSuperclass;

import java.time.LocalDateTime;

/*
 * see CommonEntityDraftInterceptor
 */
@MappedSuperclass
public interface BaseEntity {

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();
}
