package org.babyfish.jimmer.sql.example.command.common;

import org.babyfish.jimmer.sql.example.Context;

import java.util.List;
import java.util.Set;

public abstract class Command implements Context {

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
            throw new IllegalArgumentException("No enough arguments");
        }
        return args.get(index);
    }
}
