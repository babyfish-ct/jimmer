package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.model.User;

import java.util.List;
import java.util.Set;

public class Users extends Command {

    @Override
    public String getDescription() {
        return "List all users";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {
        for (User user : USER_SERVICE.findAll()) {
            System.out.println(user);
        }
    }

    @Override
    public boolean isUserRequired() {
        return false;
    }
}
