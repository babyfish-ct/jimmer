package org.babyfish.jimmer.dto.compiler;

import java.util.*;

public class DtoUtils {

    public static Collection<String> standardDtoDirs(Collection<String> dtoDirs) {
        List<String> list = new ArrayList<>();
        for (String dtoDir : dtoDirs) {
            int index = Collections.binarySearch(list, dtoDir, Comparator.comparingInt(String::length));
            if (index < 0) {
                index = -index - 1;
            }
            if (index < list.size() && dtoDir.compareTo(list.get(index)) > 0) {
                index++;
            }
            list.add(index, dtoDir);
        }
        for (int i = list.size() - 2; i >= 0; --i) {
            String path = getDtoDir(list, i);
            for (int i2 = list.size() - 1; i2 > i; --i2) {
                String path2 = getDtoDir(list, i2);
                if (path2.startsWith(path)) {
                    int len = path.length();
                    if (len == path2.length() || path2.charAt(len) == '/') {
                        list.remove(i2);
                    }
                }
            }
        }
        return list;
    }

    private static String getDtoDir(List<String> dtoDirs, int index) {
        String path = dtoDirs.get(index);
        if (path.startsWith("/")) {
            path = path.substring(1);
            dtoDirs.set(index, path);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
            dtoDirs.set(index, path);
        }
        return path;
    }
}
