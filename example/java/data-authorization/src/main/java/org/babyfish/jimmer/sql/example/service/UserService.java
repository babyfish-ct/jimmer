package org.babyfish.jimmer.sql.example.service;

import org.babyfish.jimmer.sql.example.Context;
import org.babyfish.jimmer.sql.example.model.User;
import org.babyfish.jimmer.sql.example.model.UserTable;

import java.util.List;

public class UserService implements Context {

    private final ThreadLocal<User> userLocal = new ThreadLocal<>();

    public boolean isLogged() {
        return userLocal.get() != null;
    }

    public User currentUser() {
        User user = userLocal.get();
        if (user == null) {
            throw new IllegalArgumentException("Please login");
        }
        return user;
    }

    public List<User> findAll() {
        UserTable table = UserTable.$;
        return SQL_CLIENT
                .createQuery(table)
                .orderBy(table.nickName())
                .select(table)
                .execute();
    }

    public User findByName(String nickName) {
        UserTable table = UserTable.$;
        return SQL_CLIENT
                .createQuery(table)
                .where(table.nickName().eq(nickName))
                .select(table)
                .fetchOneOrNull();
    }

    public boolean login(String nickName) {

        User user = findByName(nickName);
        if (user == null) {
            return false;
        }
        userLocal.set(user);
        return true;
    }

    public void logout() {
        userLocal.remove();
    }
}
