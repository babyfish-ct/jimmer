package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;
import org.babyfish.jimmer.sql.example.model.File;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
            List<File> roots = FILE_SERVICE.findTrees();
            for (File root : roots) {
                print(root, "", "");
            }
        } else {
            File file = FILE_SERVICE.findByPath(path);
            if (file == null) {
                throw new IllegalArgumentException("\"" + path + "\" does not exists");
            }
            file = FILE_SERVICE.findSubTree(file.id());
            print(file, "", "");
        }
    }

    private void print(File file, String prefix, String childrenPrefix) {
        PrintStream out = System.out;
        out.print(file.name());
        out.print('\n');
        for (Iterator<File> itr = file.subFiles().iterator(); itr.hasNext();) {
            File subFile = itr.next();
            if (itr.hasNext()) {
                print(subFile, childrenPrefix + "├── ", childrenPrefix + "│   ");
            } else {
                print(subFile, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }
    }
}
