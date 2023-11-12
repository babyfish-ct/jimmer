package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.model.File;
import org.babyfish.jimmer.sql.example.model.FileDraft;
import org.babyfish.jimmer.sql.example.model.FileType;

import java.util.List;
import java.util.Set;

public class Touch extends Command {

    @Override
    public String getDescription() {
        return "Create file by absolute path\n" +
                "Example:\n" +
                "-   touch /usr/local/bin/docker";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {

        String path = arg(args, 0);
        List<String> parts = FILE_SERVICE.split(path);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("The path is too short");
        }

        String parentPath = String.join("/", parts.subList(0, parts.size() - 1));
        String name = parts.get(parts.size() - 1);

        File parent = FILE_SERVICE.findByPath(parentPath);
        if (parent == null) {
            throw new IllegalArgumentException("The parent path \"" + parentPath + "\" does not exists");
        }
        if (parent.type() != FileType.DIRECTORY) {
            throw new IllegalArgumentException("The parent path \"" + parentPath + "\" is not directory");
        }

        File file = FileDraft.$.produce(draft -> {
            draft.setName(name);
            draft.applyParent(parentDraft -> parentDraft.setId(parent.id()));
            draft.setType(FileType.FILE);
        });
        FILE_SERVICE.save(file);
        System.out.println("File has been created successfully");
    }
}
