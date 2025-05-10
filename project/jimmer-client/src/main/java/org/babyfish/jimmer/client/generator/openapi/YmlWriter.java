package org.babyfish.jimmer.client.generator.openapi;

import org.babyfish.jimmer.client.generator.CodeWriter;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YmlWriter extends CodeWriter<YmlWriter> {

    private static final boolean[] SPECIAL_CHAR_FLAGS;

    public YmlWriter(Writer writer) {
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

    private static final Pattern defaultPattern = Pattern.compile("df\\(([^)]+)\\)");
    private static final Pattern patternPattern = Pattern.compile("pt\\(([^)]+)\\)");
    private static final Pattern typePattern = Pattern.compile("tp\\(([^)]+)\\)");
    private static final Pattern formatPattern = Pattern.compile("fr\\(([^)]+)\\)");

    public boolean analysisDescriptionAndReturnHaveType(Description description) {
        if (description.getSummary() != null) {
            prop("summary", description.getSummary());
        }
        String type = null;
        if (!description.getDescriptionLines().isEmpty()) {
            List<String> descriptionLines = description.getDescriptionLines();
            List<String> realDescriptionLines = new ArrayList<>();
            String defaultValue = null;
            String pattern = null;
            String format = null;

            for (String descriptionLine : descriptionLines) {
                boolean isMatched = false;
                Matcher defaultMatcher = defaultPattern.matcher(descriptionLine);
                if (defaultMatcher.find()) {
                    defaultValue = defaultMatcher.group(1);
                    isMatched = true;
                }
                if (!isMatched) {
                    Matcher patternMatcher = patternPattern.matcher(descriptionLine);
                    if (patternMatcher.find()) {
                        pattern = patternMatcher.group(1);
                        isMatched = true;
                    }
                }
                if (!isMatched) {
                    Matcher typeMatcher = typePattern.matcher(descriptionLine);
                    if (typeMatcher.find()) {
                        type = typeMatcher.group(1);
                        isMatched = true;
                    }
                }
                if (!isMatched) {
                    Matcher formatMatcher = formatPattern.matcher(descriptionLine);
                    if (formatMatcher.find()) {
                        format = formatMatcher.group(1);
                        isMatched = true;
                    }
                }
                if (!isMatched) {
                    realDescriptionLines.add(descriptionLine);
                }
            }
            code("description: ");
            if (realDescriptionLines.size() == 1) {
                code(text(realDescriptionLines.get(0))).code('\n');
            } else {
                code("|+");
                scope(CodeWriter.ScopeType.BLANK, "", true, () -> {
                    for (String line : realDescriptionLines) {
                        code(line).code('\n');
                    }
                });
            }
            if (defaultValue != null) {
                code("default: ");
                code(defaultValue).code('\n');
                if (pattern == null) {
                    String defaultPattern = DatePatternDetector.detectPattern(defaultValue);
                    if (defaultPattern != null) {
                        code("pattern: ");
                        code(defaultPattern).code('\n');
                    }
                }
            }
            if (pattern != null) {
                code("pattern: ");
                code(pattern).code('\n');
            }
            if (type != null) {
                code("type: ");
                code(type).code('\n');
            }
            if (format != null) {
                code("format: ");
                code(format).code('\n');
            }
        }
        return type!=null;
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
