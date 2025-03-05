package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.link.Course;
import org.babyfish.jimmer.sql.model.link.CourseDependencyFetcher;
import org.babyfish.jimmer.sql.model.link.CourseFetcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RecursiveViewFetcherTest extends Tests {

    @Test
    public void testValidMerge() {
        Fetcher<Course> fetcher = CourseFetcher.$
                .prevCourseDependencies(
                        CourseDependencyFetcher.$
                                .reason()
                )
                .recursiveNextCourses();
        Field field = fetcher.getFieldMap().get("nextCourses");
        Assertions.assertFalse(field.isSimpleField());
        assertContentEquals(
                "org.babyfish.jimmer.sql.model.link.Course { " +
                        "--->id, " +
                        "--->prevCourseDependencies { " +
                        "--->--->id, " +
                        "--->--->reason " +
                        "--->}, " +
                        "--->nextCourses(recursive: true) " +
                        "}",
                fetcher
        );
    }

    @Test
    public void testIllegalMerge1() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {
                    CourseFetcher.$
                            .prevCourseDependencies(
                                    CourseDependencyFetcher.$
                                            .reason()
                            )
                            .recursivePrevCourses();
                }
        );
        assertContentEquals(
                "The many-to-many-view property " +
                        "\"org.babyfish.jimmer.sql.model.link.Course.prevCourses\" " +
                        "cannot be fetched recursively, " +
                        "please fetch it non-recursively " +
                        "because its base association " +
                        "\"org.babyfish.jimmer.sql.model.link.Course.prevCourseDependencies\" " +
                        "has be fetched",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalMerge2() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {
                    CourseFetcher.$
                            .recursivePrevCourses()
                            .prevCourseDependencies(
                                    CourseDependencyFetcher.$
                                            .reason()
                            );
                }
        );
        assertContentEquals(
                "The association property " +
                        "\"org.babyfish.jimmer.sql.model.link.Course.prevCourseDependencies\" " +
                        "cannot be fetched because its many-to-many-view association " +
                        "\"org.babyfish.jimmer.sql.model.link.Course.prevCourses\" " +
                        "has be recursively fetched",
                ex.getMessage()
        );
    }
}
