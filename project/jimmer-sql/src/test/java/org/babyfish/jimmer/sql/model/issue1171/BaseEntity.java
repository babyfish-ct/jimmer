package org.babyfish.jimmer.sql.model.issue1171;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * @author gf
 */
@MappedSuperclass
public interface BaseEntity {
    /**
     * 创建部门
     */
    @Nullable
    @Column(name = "create_dept")
    Long createDept();

    /**
     * 创建者
     */
    @Nullable
    @Column(name = "create_by")
    Long createBy();

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Nullable
    @Column(name = "create_time")
    LocalDateTime createTime();

    /**
     * 更新者
     */
    @Nullable
    @Column(name = "update_by")
    Long updateBy();

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Nullable
    @Column(name = "update_time")
    LocalDateTime updateTime();


}

