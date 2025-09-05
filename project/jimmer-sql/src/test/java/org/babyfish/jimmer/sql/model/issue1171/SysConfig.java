package org.babyfish.jimmer.sql.model.issue1171;

import java.lang.String;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;
import org.jetbrains.annotations.Nullable;

/**
 * 参数配置表
 */
@Entity
@Table(name = "issue1171_sys_config")
public interface SysConfig extends BaseEntity {
    /**
     * 参数主键
     */
    @Column(name = "config_id")
    @Id
    long configId();

    /**
     * 参数名称
     */
    @Column(name = "config_name")
    @Nullable
    String configName();

    /**
     * 参数键名
     */
    @Column(name = "config_key")
    @Nullable
    String configKey();

    /**
     * 参数键值
     */
    @Column(name = "config_value")
    @Nullable
    String configValue();

    /**
     * 系统内置（Y是 N否）
     */
    @Column(name = "config_type")
    @Nullable
    String configType();

    /**
     * 备注
     */
    @Column(name = "remark")
    @Nullable
    String remark();
}

