package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;

import java.util.List;
import java.util.Set;

public class Cache extends Command {

    @Override
    public String getDescription() {
        return "Print all items of cache";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {
        CACHE_STORAGE.trace();
    }

    @Override
    public boolean isUserRequired() {
        return false;
    }
}
