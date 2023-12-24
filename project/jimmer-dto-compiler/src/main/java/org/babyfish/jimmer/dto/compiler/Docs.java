package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class Docs {

    public static String parse(Token token) {
        if (token == null) {
            return null;
        }
        String doc = token.getText().trim();
        if (doc.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(doc));
        boolean prevLineEmpty = true;
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                int startIndex = line.indexOf('*') + 1;
                while (startIndex < line.length()) {
                    char c = line.charAt(startIndex);
                    if (c > ' ' && c != '*') {
                        break;
                    }
                    startIndex++;
                }
                int endIndex = line.length() - (line.endsWith("*/") ? 2 : 0);
                if (startIndex < endIndex) {
                    builder.append(line, startIndex, endIndex).append('\n');
                    prevLineEmpty = false;
                } else {
                    if (!prevLineEmpty) {
                        builder.append('\n');
                    }
                    prevLineEmpty = true;
                }
            }
        } catch (IOException ex) {
            throw new AssertionError("Internal bug: cannot read line from string reader");
        }
        for (int i = builder.length() - 1; i >= 0; --i) {
            if (builder.charAt(i) > ' ') {
                builder.setLength(i + 1);
                break;
            }
        }
        return builder.toString();
    }
}
