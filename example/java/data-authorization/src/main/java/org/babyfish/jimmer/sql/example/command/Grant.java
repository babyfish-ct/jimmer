package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.command.common.CommandException;
import org.babyfish.jimmer.sql.example.model.File;
import org.babyfish.jimmer.sql.example.model.User;

import java.util.List;
import java.util.Set;

public class Grant extends Command {

    @Override
    public String getDescription() {
        return "Grant a file to an user\n" +
                "Notes: If grant a file to an user, all the parents will also be recursively granted too\n" +
                "Example:\n" +
                "-   grant /usr/local/bin/docker alex\n";
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

        FILE_SERVICE.grant(file.id(), user.id());
        System.out.println("Granted successfully");
    }
}
