package org.babyfish.jimmer.sql.example.graphql.entities.common;

import org.babyfish.jimmer.sql.MappedSuperclass;

import java.time.LocalDateTime;

/*
 * set CommonEntityDraftInterceptor
 */
@MappedSuperclass
public interface BaseEntity {

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();
}
