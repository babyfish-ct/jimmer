package org.babyfish.jimmer.sql.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Console {

    private Console() {}

    public static String readLine(String format, Object ... args) {

        if (System.console() != null) {
            return System.console().readLine(format, args);
        }

        // Boring code when System.console() is null

        System.out.print(String.format(format, args));
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Cannot not read line from System.in");
        }
    }
}
