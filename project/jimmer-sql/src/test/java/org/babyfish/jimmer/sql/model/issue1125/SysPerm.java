package org.babyfish.jimmer.sql.model.issue1125;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

@Entity(microServiceName = "perm-service")
@Table(name = "issue1125_sys_perm")
public interface SysPerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Nullable
    @LogicalDeleted("now")
    @Column(name = "deleted_at")
    LocalDateTime deletedAt();

    @ManyToMany
    @JoinTable(
            name = "issue1125_mp_role_perm",
            joinColumnName = "perm_id",
            inverseJoinColumnName = "role_id",
            logicalDeletedFilter =
            @JoinTable.LogicalDeletedFilter(
                    columnName = "deleted_at",
                    type = LocalDateTime.class,
                    nullable = true,
                    value = "now"
            )
    )
    List<SysRole> roles();
}
