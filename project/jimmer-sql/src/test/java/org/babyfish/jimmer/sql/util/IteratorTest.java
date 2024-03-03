package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.ast.impl.util.ConcattedIterator;
import org.babyfish.jimmer.sql.ast.impl.util.FlaternIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class IteratorTest {

    @Test
    public void testConcat() {
        Iterator<Integer> itr = ConcattedIterator.of(
                Collections.emptyIterator(),
                Arrays.asList(1, 4).iterator(),
                Collections.emptyIterator(),
                Arrays.asList(9, 16).iterator(),
                Collections.emptyIterator(),
                Arrays.asList(25, 36).iterator(),
                Collections.emptyIterator()
        );
        StringBuilder builder = new StringBuilder();
        while (itr.hasNext()) {
            builder.append(itr.next()).append(':');
        }
        Assertions.assertEquals("1:4:9:16:25:36:", builder.toString());
    }

    @Test
    public void testFlat() {
        Iterator<Integer> itr = new FlaternIterator<>(
                Arrays.asList(
                        Collections.<Integer>emptyList(),
                        Arrays.asList(1, 4),
                        Collections.<Integer>emptyList(),
                        Arrays.asList(9, 16),
                        Collections.<Integer>emptyList(),
                        Arrays.asList(25, 36),
                        Collections.<Integer>emptyList()
                ).iterator()
        );
        StringBuilder builder = new StringBuilder();
        while (itr.hasNext()) {
            builder.append(itr.next()).append(':');
        }
        Assertions.assertEquals("1:4:9:16:25:36:", builder.toString());
    }
}
