package org.babyfish.jimmer.sql.example;

import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

public class App implements Context {

    public static void main(String[] args) {

        TRANSACTION_MANAGER.execute(() -> {
            ((JSqlClientImplementor)SQL_CLIENT).initialize();
            return 0;
        });

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
