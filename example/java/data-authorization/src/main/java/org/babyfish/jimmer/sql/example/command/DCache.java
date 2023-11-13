package org.babyfish.jimmer.sql.example.command;

import org.babyfish.jimmer.sql.example.command.common.Command;

import java.util.List;
import java.util.Set;

public class DCache extends Command {

    @Override
    public String getDescription() {
        return "Clear all items of cache";
    }

    @Override
    public void execute(Set<Character> flags, List<String> args) {
        CACHE_STORAGE.clear();
        System.out.println("All items of cache is deleted");
    }

    @Override
    public boolean isUserRequired() {
        return false;
    }
}
