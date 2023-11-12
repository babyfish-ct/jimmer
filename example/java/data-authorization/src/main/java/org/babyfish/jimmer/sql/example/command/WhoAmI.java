package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.model.User;

import java.util.List;
import java.util.Set;

public class WhoAmI extends Command {

    @Override
    public String getDescription() {
        return "The nick name of current user";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {
        if (!USER_SERVICE.isLogged()) {
            System.out.println("<NOT LOGGED>");
        } else {
            User user = USER_SERVICE.currentUser();
            System.out.println(user.nickName());
        }
    }

    @Override
    public boolean isUserRequired() {
        return false;
    }
}
