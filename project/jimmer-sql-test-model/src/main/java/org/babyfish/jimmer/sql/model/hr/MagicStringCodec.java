package org.babyfish.jimmer.sql.model.hr;

public class MagicStringCodec {
    public static String serialize(String value) {
        StringBuilder builder = new StringBuilder();
        int size = value.length();
        for (int i = 0; i < size; i++) {
            builder.append((char) (value.charAt(i) + 1));
        }
        return builder.toString();
    }

    public static String deserialize(String value) {
        StringBuilder builder = new StringBuilder();
        int size = value.length();
        for (int i = 0; i < size; i++) {
            builder.append((char) (value.charAt(i) - 1));
        }
        return builder.toString();
    }
}
