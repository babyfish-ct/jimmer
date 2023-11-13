package org.babyfish.jimmer.sql.example.command.common;

import org.babyfish.jimmer.sql.example.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

public class CommandDispatcher implements Context {

    private static final Pattern BLANK_PATTERN = Pattern.compile("\\s+");

    private final Map<String, Command> commandMap;

    public CommandDispatcher(Command ... commands) {
        Map<String, Command> map = new LinkedHashMap<>((commands.length * 4 + 2) / 3);
        for (Command command : commands) {
            map.put(command.getName(), command);
        }
        this.commandMap = map;
    }

    public boolean execute() {
        String line = readInput();
        if (line == null || line.isEmpty()) {
            return true;
        }
        String[] parts = BLANK_PATTERN.split(line);
        if (parts.length == 0) {
            return true;
        }
        String name = parts[0];
        if (name.equals("exit")) {
            return false;
        }
        if (name.equals("help")) {
            help();
            return true;
        }
        Command command = commandMap.get(name);
        if (command == null) {
            System.out.println("Illegal command: " + name);
            return true;
        }

        TRANSACTION_MANAGER.execute(() -> {
            try {
                execute(command, parts);
            } catch (CommandException ex) {
                System.out.println(ex.getMessage());
            } catch (RuntimeException | Error ex) {
                ex.printStackTrace(System.err);
            }
            return null;
        });

        return true;
    }

    private void help() {

        System.out.println("-   help");
        System.out.println("    Show all commands");
        System.out.println();

        System.out.println("-   exit");
        System.out.println("    Exit the application");
        System.out.println();

        for (Command command : commandMap.values()) {
            System.out.print("-   ");
            System.out.println(command.getName());
            String description = command.getDescription();
            if (description != null && !description.isEmpty()) {
                description = description.trim().replace("\n", "\n    ");
                System.out.print("    ");
                System.out.println(description);
            }
            System.out.println();
        }
    }

    private static String readInput() {
        if (USER_SERVICE.isLogged()) {
            System.out.print(USER_SERVICE.currentUser().nickName());
            System.out.print('@');
        }
        System.out.print("mocked-fs > ");
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException ex) {
            throw new AssertionError("stdin error");
        }
    }

    private static void execute(Command command, String[] parts) {

        if (command.isUserRequired() && !USER_SERVICE.isLogged()) {
            System.out.println("Error: You've not logged, please login");
            return;
        }

        Set<Character> flags = new HashSet<>();
        List<String> args = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("-")) {
                for (int ii = parts[i].length() - 1; ii > 0; --ii) {
                    char flag = parts[i].charAt(ii);
                    if (flag == 'h') {
                        System.out.println(command.getDescription());
                        return;
                    }
                    flags.add(flag);
                }
            } else {
                args.add(parts[i]);
            }
        }

        command.execute(flags, args);
    }
}
