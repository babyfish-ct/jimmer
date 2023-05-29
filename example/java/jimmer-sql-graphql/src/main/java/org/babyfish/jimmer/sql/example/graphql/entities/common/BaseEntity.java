package org.babyfish.jimmer.sql.example.graphql.entities.common;

import org.babyfish.jimmer.sql.MappedSuperclass;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/*
 * set CommonEntityDraftInterceptor
 */
@MappedSuperclass
public interface BaseEntity {

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdTime();

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime modifiedTime();
}
