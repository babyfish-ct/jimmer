package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;

import java.util.List;
import java.util.Set;

public class Logout extends Command {

    @Override
    public void execute(Set<Character> flags, List<String> args) {
        USER_SERVICE.logout();
        System.out.println("Logged out successfully");
    }

    @Override
    public boolean isUserRequired() {
        return false;
    }
}
