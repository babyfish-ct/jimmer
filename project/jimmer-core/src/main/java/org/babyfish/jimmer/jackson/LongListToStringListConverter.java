package org.babyfish.jimmer.jackson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LongListToStringListConverter implements Converter<List<Long>, List<String>> {

    @NotNull
    @Override
    public List<String> output(@NotNull List<Long> value) {
        List<String> list = new ArrayList<>(value.size());
        for (Long l : value) {
            list.add(Long.toString(l));
        }
        return list;
    }

    @NotNull
    @Override
    public List<Long> input(@NotNull List<String> jsonValue) {
        List<Long> list = new ArrayList<>(jsonValue.size());
        for (String s : jsonValue) {
            list.add(Long.parseLong(s));
        }
        return list;
    }
}
