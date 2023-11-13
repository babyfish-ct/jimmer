package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.command.common.CommandException;
import org.babyfish.jimmer.sql.example.model.File;
import org.babyfish.jimmer.sql.example.model.User;

import java.util.List;
import java.util.Set;

public class Revoke extends Command {

    @Override
    public String getDescription() {
        return "Revoke a file from an user" +
                "Notes: If revoke a file from an user, all the child files will also be recursively revoked too\n" +
                "Example " +
                "-   revoke /usr/local/bin/docker alex";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {
        String path = arg(args, 0);
        String nickName = arg(args, 1);
        File file = getFile(path, null);
        User user = USER_SERVICE.findByName(nickName);
        if (user == null) {
            throw new CommandException("The user \"" + nickName + "\"does not exists");
        }
        FILE_SERVICE.revoke(file.id(), user.id());
        System.out.println("Revoked successfully");
    }
}
