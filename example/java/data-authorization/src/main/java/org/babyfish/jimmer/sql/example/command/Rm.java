package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.command.common.CommandException;
import org.babyfish.jimmer.sql.example.model.File;

import java.util.List;
import java.util.Set;

public class Rm extends Command {

    @Override
    public String getDescription() {
        return "Remove file by absolute path\n" +
                "A optional flag `-r`, means removing file recursively\n" +
                "Examples:\n" +
                "-   rm /usr/local/bin/docker\n" +
                "-   rm -r /usr/local/bin/docker";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {

        String path = arg(args, 0);
        File file = getFile(path, null);

        FILE_SERVICE.delete(file.id(), flags.contains('r'));
        System.out.println("Deleted successfully");
    }
}
