package org.babyfish.jimmer.apt.entry;

import java.util.HashMap;
import java.util.Map;

public class AbstractSummaryGenerator {

    private Map<String, Integer> nameCountMap = new HashMap<>();

    protected String distinctName(String name) {
        int count = nameCountMap.getOrDefault(name, 1);
        nameCountMap.put(name, count + 1);
        if (count == 1) {
            return name;
        }
        return name + '_' + count;
    }
}
