package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.ast.impl.util.ConcattedIterator;
import org.babyfish.jimmer.sql.ast.impl.util.FlaternIterator;
import org.babyfish.jimmer.sql.ast.impl.util.InList;
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

    @Test
    public void testInList() {
        InList<Integer> list = new InList<>(
                Arrays.asList(
                        1, 4, 9, 16, 25, 36, 49,
                        64, 81, 100
                ),
                true,
                7
        );
        StringBuilder builder = new StringBuilder();
        for (Iterable<Integer> iterable : list) {
            for (Integer i : iterable) {
                builder.append(i).append(' ');
            }
            builder.append('\n');
        }
        Assertions.assertEquals(
                "1 4 9 16 25 36 49 \n" +
                        "64 81 100 100 \n",
                builder.toString()
        );
    }

    @Test
    public void testInListWithCommitter() {
        InList<Integer> list = new InList<>(
                Arrays.asList(
                        null, 1, 4, 9, 16, 25, null, 36, null, 49, null,
                        null, 64, 81, null, 100, null
                ),
                true,
                7
        );
        InList.Committer committer = list.committer();
        StringBuilder builder = new StringBuilder();
        for (Iterable<Integer> iterable : list) {
            for (Integer i : iterable) {
                if (i != null) {
                    builder.append(i).append(' ');
                    committer.commit();
                }
            }
            builder.append('\n');
        }
        Assertions.assertEquals(
                "1 4 9 16 25 36 49 \n" +
                        "64 81 100 100 \n",
                builder.toString()
        );
    }

    @Test
    public void testEmptyInListWithCommitter() {
        InList<Integer> list = new InList<>(
                Arrays.asList(
                        null, null, null
                ),
                true,
                Integer.MAX_VALUE
        );
        InList.Committer committer = list.committer();
        StringBuilder builder = new StringBuilder();
        for (Iterable<Integer> iterable : list) {
            for (Integer i : iterable) {
                if (i != null) {
                    builder.append(i).append(' ');
                    committer.commit();
                }
            }
            builder.append('\n');
        }
        Assertions.assertEquals(
                "\n",
                builder.toString()
        );
    }
}
