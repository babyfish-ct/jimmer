package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.model.*;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Tree extends Command {

    @Override
    public String getDescription() {
        return "View the tree of file system\n" +
                "You can call it by two ways\n" +
                "-   View all root trees\n" +
                "    tree\n" +
                "-   View sub tree by absolute path\n" +
                "    tree /usr/local";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {
        String path = args.isEmpty() ? null : args.get(0);
        if (path == null) {
            List<File> roots = FILE_SERVICE.findRootDetailTrees();
            File virtualRoot = FileDraft.$.produce(draft -> {
                draft.setName("/");
                draft.setSubFiles(roots);
                draft.setAuthorizedUsers(Collections.emptyList());
            });
            print(virtualRoot, "", "");
        } else {
            File file = getFile(path, null);
            file = FILE_SERVICE.findSubDetailTree(file.id());
            print(file, "", "");
        }
    }

    private void print(File file, String prefix, String childrenPrefix) {
        PrintStream out = System.out;
        out.print(prefix);
        out.print(file.name());
        printDetail(file);
        out.println();
        for (Iterator<File> itr = file.subFiles().iterator(); itr.hasNext();) {
            File subFile = itr.next();
            if (itr.hasNext()) {
                print(subFile, childrenPrefix + "├── ", childrenPrefix + "│   ");
            } else {
                print(subFile, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }
    }

    private void printDetail(File file) {
        if (!ImmutableObjects.isLoaded(file, FileProps.TYPE)) { // virtual root
            return;
        }
        System.out.print(file.type() == FileType.FILE ? " \u001B[32m<" : " \u001B[36m<");
        System.out.print(file.type() == FileType.FILE ? "D: " : "F: ");
        System.out.print(
                file
                        .authorizedUsers()
                        .stream()
                        .map(User::nickName)
                        .collect(Collectors.joining(","))
        );
        System.out.print(">\u001B[0m");
    }
}
