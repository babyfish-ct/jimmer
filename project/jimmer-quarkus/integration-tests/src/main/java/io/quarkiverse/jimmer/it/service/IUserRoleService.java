package io.quarkiverse.jimmer.it.service;

import io.quarkiverse.jimmer.it.entity.UserRole;

public interface IUserRoleService {

    UserRole findById(String id);
}
