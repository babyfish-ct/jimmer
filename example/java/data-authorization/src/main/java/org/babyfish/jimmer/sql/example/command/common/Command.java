package org.babyfish.jimmer.sql.example.command.common;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.Context;
import org.babyfish.jimmer.sql.example.model.File;
import org.babyfish.jimmer.sql.example.service.NotExistsException;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class Command implements Context {

    private static final Pattern SLASH_PATTERN = Pattern.compile("/");

    public String getName() {
        return getClass().getSimpleName().toLowerCase();
    }

    public String getDescription() {
        return null;
    }

    public abstract void execute(Set<Character> flags, List<String> args);

    public boolean isUserRequired() {
        return true;
    }

    protected static String arg(List<String> args, int index) {
        if (index >= args.size()) {
            throw new CommandException("No enough arguments");
        }
        return args.get(index);
    }

    protected static Tuple2<String, String> split(String path) {
        List<String> parts = Arrays.stream(SLASH_PATTERN.split(path))
                .filter(it -> !it.isEmpty())
                .collect(Collectors.toList());
        switch (parts.size()) {
            case 0:
                return new Tuple2<>(null, null);
            case 1:
                return new Tuple2<>(null, parts.get(0));
            default:
                return new Tuple2<>(
                        '/' + String.join("/", parts.subList(0, parts.size() - 1)),
                        parts.get(parts.size() - 1)
                );
        }
    }

    protected static File getFile(String path, String pathArgumentName) {
        try {
            return FILE_SERVICE.findByPath(path);
        } catch (NotExistsException ex) {
            throw new CommandException(
                    "Illegal " +
                            (pathArgumentName != null ? pathArgumentName : "path") +
                            " \"" +
                            path +
                            "\", the \"" +
                            ex.getErrorPath() +
                            "\" does not exists"
            );
        }
    }
}
