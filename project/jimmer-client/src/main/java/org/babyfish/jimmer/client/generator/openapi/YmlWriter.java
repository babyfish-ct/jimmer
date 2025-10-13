package org.babyfish.jimmer.client.generator.openapi;

import org.babyfish.jimmer.client.generator.CodeWriter;

public class YmlWriter extends CodeWriter<YmlWriter> {

    private static final boolean[] SPECIAL_CHAR_FLAGS;

    public YmlWriter(Appendable writer) {
        super("  ");
        setWriter(writer);
    }

    public YmlWriter object(String name, Runnable block) {
        code(name).code(':');
        return scope(ScopeType.BLANK, "", true, block);
    }

    public YmlWriter list(String name, Runnable block) {
        code(name).code(':');
        scope(ScopeType.BLANK, "", true, block);
        return this;
    }

    public YmlWriter listItem(Runnable block) {
        code("- ");
        scope(ScopeType.BLANK, "", false, block);
        return this;
    }

    public YmlWriter prop(String name, String value) {
        if (value != null && !value.isEmpty()) {
            code(name).code(": ").code(text(value)).code('\n');
        }
        return this;
    }

    public YmlWriter description(Description description) {
        if (description.getSummary() != null) {
            prop("summary", description.getSummary());
        }
        if (!description.getDescriptionLines().isEmpty()) {
            code("description: ");
            if (description.getDescriptionLines().size() == 1) {
                code(text(description.getDescriptionLines().get(0))).code('\n');
            } else {
                code("|+");
                scope(ScopeType.BLANK, "", true, () -> {
                    for (String line : description.getDescriptionLines()) {
                        code(line).code('\n');
                    }
                });
            }
        }
        return this;
    }

    private static String text(String line) {
        boolean complex = line.charAt(0) == '\'';
        if (!complex) {
            for (int i = line.length() - 1; i >= 0; --i) {
                char c = line.charAt(i);
                if (c < SPECIAL_CHAR_FLAGS.length && SPECIAL_CHAR_FLAGS[c]) {
                    complex = true;
                    break;
                }
            }
        }
        if (!complex) {
            return line;
        }
        return '\'' +
                line
                        .replace("\\", "\\\\")
                        .replace("\n", "\\n")
                        .replace("'", "''") +
                '\'';
    }

    static {
        boolean[] arr = new boolean[128];
        String specialChars = ":{}[],&*#?|-<>=!%@`";
        for (int i = specialChars.length() - 1; i >= 0; --i) {
            arr[specialChars.charAt(i)] = true;
        }
        SPECIAL_CHAR_FLAGS = arr;
    }
}
