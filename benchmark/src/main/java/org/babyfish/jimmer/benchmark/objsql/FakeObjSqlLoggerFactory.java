package org.babyfish.jimmer.benchmark.objsql;

import com.github.braisdom.objsql.Databases;
import com.github.braisdom.objsql.Logger;
import com.github.braisdom.objsql.LoggerFactory;

/*
 * By default ObjectiveSQL prints SQL, which is very slow, turn it off
 */
public class FakeObjSqlLoggerFactory implements LoggerFactory {

    private FakeObjSqlLoggerFactory() {}

    public static void init() {
        Databases.installLoggerFactory(new FakeObjSqlLoggerFactory());
    }


    @Override
    public Logger create(Class<?> clazz) {
        return new Logger() {

            @Override
            public void debug(long elapsedTime, String sql, Object[] params) {

            }

            @Override
            public void info(long elapsedTime, String sql, Object[] params) {

            }

            @Override
            public void error(String message, Throwable throwable) {

            }
        };
    }
}
