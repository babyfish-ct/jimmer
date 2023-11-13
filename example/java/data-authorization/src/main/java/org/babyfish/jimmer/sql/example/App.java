package org.babyfish.jimmer.sql.example;

public class App implements Context {

    public static void main(String[] args) {

        Context.initialize();

        System.out.println("---------------------------------------------------------");
        System.out.println("You can enter `help` to know which commands are supported");
        System.out.println("---------------------------------------------------------");

        // `while (COMMAND_DISPATCHER.execute());` will cause warning of Intellij

        while (true) {
            if (!COMMAND_DISPATCHER.execute()) {
                break;
            }
        }
    }
}
