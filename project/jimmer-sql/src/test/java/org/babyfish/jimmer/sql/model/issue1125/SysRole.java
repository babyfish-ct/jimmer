package org.babyfish.jimmer.sql.model.issue1125;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

@Entity(microServiceName = "perm-service")
@Table(name = "issue1125_sys_role")
public interface SysRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Nullable
    @LogicalDeleted("now")
    @Column(name = "deleted_at")
    LocalDateTime deletedAt();

    @ManyToMany(mappedBy = "roles")
    List<SysPerm> perms();
}