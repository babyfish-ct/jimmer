package io.quarkiverse.jimmer.it.service.impl;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.jimmer.it.entity.UserRole;
import io.quarkiverse.jimmer.it.service.IUserRoleService;
import org.babyfish.jimmer.quarkus.runtime.Jimmer;

@ApplicationScoped
public class UserRoleServiceImpl implements IUserRoleService {

    private final String DB2 = "DB2";

    private final Jimmer jimmer;

    public UserRoleServiceImpl(Jimmer jimmer) {
        this.jimmer = jimmer;
    }

    @Override
    public UserRole findById(String id) {
        return jimmer.getJSqlClient(DB2).findById(UserRole.class, id);
    }
}
