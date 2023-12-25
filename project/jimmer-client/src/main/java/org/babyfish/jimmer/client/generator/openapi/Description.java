package org.babyfish.jimmer.client.generator.openapi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Description {

    private static final Description NIL = new Description(null, Collections.emptyList());

    private final String summary;

    private final List<String> descriptionLines;

    public Description(String summary, List<String> descriptionLines) {
        this.summary = summary;
        this.descriptionLines = descriptionLines;
    }

    @Nullable
    public String getSummary() {
        return summary;
    }

    @NotNull
    public List<String> getDescriptionLines() {
        return descriptionLines;
    }

    public static Description of(String doc) {
        return of(doc, false);
    }

    public static Description of(String doc, boolean extractSummary) {
        if (doc == null || doc.isEmpty()) {
            return NIL;
        }
        boolean summaryExtracted = !extractSummary;
        String summary = null;
        List<String> descriptionLines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(doc));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                boolean notBlank = false;
                int len = line.length();
                for (int i = 0; i < len; i++) {
                    if (line.charAt(i) > ' ') {
                        notBlank = true;
                        break;
                    }
                }
                if (!notBlank) {
                    continue;
                }
                if (summaryExtracted) {
                    descriptionLines.add(line);
                } else {
                    summary = line;
                    summaryExtracted = true;
                }
            }
        } catch (IOException ex) {
            throw new AssertionError("Internal bug: Cannot read data from string reader", ex);
        }
        return new Description(summary, Collections.unmodifiableList(descriptionLines));
    }
}
