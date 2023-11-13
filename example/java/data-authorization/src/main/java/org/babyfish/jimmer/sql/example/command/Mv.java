package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.command.common.CommandException;
import org.babyfish.jimmer.sql.example.model.File;
import org.babyfish.jimmer.sql.example.model.FileDraft;
import org.babyfish.jimmer.sql.example.model.FileType;

import java.util.List;
import java.util.Set;

public class Mv extends Command {

    @Override
    public String getDescription() {
        return "Move file/directory to other directory or file(move + rename)\n" +
                "Examples:\n" +
                "-   mv /usr/alex/prj_01/hello_world.cpp /usr/alex/prj_02\n" +
                "-   mv /usr/alex/prj_01/hello_world.cpp /usr/alex/prj_02/simple.cpp\n" +
                "-   mv /usr/alex/prj_01 /usr/alex/prj_02\n" +
                "Notes: 1. Does not support wildcard `., *`, 2. `-r` is unnecessary\n";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {
        String source = arg(args, 0);
        String target = arg(args, 1);

        File sourceFile = getFile(source, "source path");
        if (!FILE_SERVICE.isGranted(sourceFile.id(), USER_SERVICE.currentUser().id())) {
            throw new CommandException(
                    "The source path \"" + source + "\" cannot be moved because it has not been granted to you"
            );
        }

        Tuple2<String, String> tuple = split(target);
        String targetParent = '/' + tuple.get_1();
        String targetName = tuple.get_2();
        File targetDir = getFile(targetParent, "target path");

        String newName;
        if (targetDir == null) { // Move to root
            newName = sourceFile.name();
        } else {
            File deeperTargetFile = FILE_SERVICE.findByParentIdAndName(targetDir.id(), targetName);
            if (deeperTargetFile == null) {
                // Move and rename
                // mv /usr/alex/prj_01/hello_world.cpp /usr/alex/prj_02/new_name.cpp
                newName = targetName;
                if (targetDir.type() != FileType.DIRECTORY) {
                    throw new CommandException("The parent of target path is \"" + targetParent + "\", it is not directory");
                }
            } else if (deeperTargetFile.type() == FileType.DIRECTORY) {
                // Move, but not remove,
                // mv /usr/alex/prj_01/hello_world.cpp /usr/alex/prj_02
                newName = sourceFile.name();
                targetDir = deeperTargetFile;
                targetParent += '/';
                targetParent += targetName;
            } else {
                throw new CommandException("The target path \"" + target + "\" already exists");
            }
            if (!FILE_SERVICE.isGranted(targetDir.id(), USER_SERVICE.currentUser().id())) {
                throw new CommandException(
                        "The directory \"" + targetParent + "\" cannot be used as target directory because it is not granted to you"
                );
            }
            if (sourceFile.id() != targetDir.id()) { // Not only rename
                File sourceTree = FILE_SERVICE.findSubDetailTree(sourceFile.id());
                validateDeadRecursion(sourceTree, targetDir.id());
            }
        }

        File idOnlyTarget = targetDir != null ? ImmutableObjects.makeIdOnly(File.class, targetDir.id()) : null;
        File file = FileDraft.$.produce(sourceFile, draft -> {
            draft.setId(sourceFile.id());
            draft.setName(newName);
            draft.setParent(idOnlyTarget);
        });
        FILE_SERVICE.save(file);

        System.out.print("File has been moved successfully");
    }

    private void validateDeadRecursion(File sourceTree, long targetDirId) {
        if (sourceTree.id() == targetDirId) {
            throw new CommandException(
                    "Cannot move file to its descendant"
            );
        }
        for (File subFile : sourceTree.subFiles()) {
            validateDeadRecursion(subFile, targetDirId);
        }
    }
}
