package org.babyfish.jimmer.jackson;

public class ClassUtils {
    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
