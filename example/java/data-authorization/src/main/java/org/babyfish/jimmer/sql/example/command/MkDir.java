package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.command.common.CommandException;
import org.babyfish.jimmer.sql.example.model.File;
import org.babyfish.jimmer.sql.example.model.FileDraft;
import org.babyfish.jimmer.sql.example.model.FileType;
import org.babyfish.jimmer.sql.example.service.FileService;

import java.util.List;
import java.util.Set;

public class MkDir extends Command {

    @Override
    public String getDescription() {
        return "create directory by absolute path\n" +
                "Example:\n" +
                "-   mkdir /usr/local/bin\n";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {

        String path = arg(args, 0);
        Tuple2<String, String> tuple = split(path);
        String parentPath = tuple.get_1();
        String name = tuple.get_2();
        if (name == null) {
            return;
        }

        File parent = parentPath != null ? getFile(parentPath, null) : null;
        if (parent == null && parentPath != null) {
            throw new CommandException("The parent path \"" + parentPath + "\" does not exists");
        }
        if (parent != null && parent.type() != FileType.DIRECTORY) {
            throw new CommandException("The parent path \"" + parentPath + "\" is not directory");
        }
        Long parentId = parent != null ? parent.id() : null;
        if (FILE_SERVICE.findByParentIdAndName(parentId, name) != null) {
            throw new CommandException("The path \"" + path + "\" already exists");
        }

        File idOnlyParent = parentId != null ? ImmutableObjects.makeIdOnly(File.class, parentId) : null;
        File file = FileDraft.$.produce(draft -> {
            draft.setName(name);
            draft.setParent(idOnlyParent);
            draft.setType(FileType.DIRECTORY);
        });
        FILE_SERVICE.save(file);

        System.out.println("Directory has been created successfully");
    }
}
