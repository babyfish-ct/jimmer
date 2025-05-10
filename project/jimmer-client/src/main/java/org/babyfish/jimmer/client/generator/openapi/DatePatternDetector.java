package org.babyfish.jimmer.client.generator.openapi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatePatternDetector {

    private static final Map<Pattern, String> DATE_PATTERNS = new LinkedHashMap<>();

    static {
        // 高精度带时区的格式（优先匹配）
        DATE_PATTERNS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3,9}[+-]\\d{2}:\\d{2}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        DATE_PATTERNS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$"), "yyyy-MM-dd'T'HH:mm:ss'Z'");

        // 新增：匹配不带毫秒和时区的 ISO 8601 格式
        DATE_PATTERNS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$"), "yyyy-MM-dd'T'HH:mm:ss");

        // 标准日期时间格式
        DATE_PATTERNS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"), "yyyy-MM-dd HH:mm:ss");
        DATE_PATTERNS.put(Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}$"), "yyyy/MM/dd HH:mm:ss");
        DATE_PATTERNS.put(Pattern.compile("^\\d{2}-[A-Za-z]{3}-\\d{4} \\d{2}:\\d{2}:\\d{2}$"), "dd-MMM-yyyy HH:mm:ss");

        // ... 其他已有规则
    }

    public static String detectPattern(String dateString) {
        for (Map.Entry<Pattern, String> entry : DATE_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(dateString);
            if (matcher.matches()) {
                return entry.getValue();
            }
        }
        return null;
    }
}