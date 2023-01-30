package org.babyfish.jimmer.sql2.example.model.common;

import org.babyfish.jimmer.pojo.AutoScalarRule;
import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.time.LocalDateTime;

/*
 * see CommonEntityDraftInterceptor
 */
@MappedSuperclass
@AutoScalarRule(AutoScalarStrategy.NONE)
public interface BaseEntity {

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();
}
