package org.babyfish.jimmer.sql.example;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.di.AbstractJSqlClientWrapper;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.example.cache.CacheFactoryImpl;
import org.babyfish.jimmer.sql.example.cache.CacheStorage;
import org.babyfish.jimmer.sql.example.command.*;
import org.babyfish.jimmer.sql.example.command.common.CommandDispatcher;
import org.babyfish.jimmer.sql.example.database.ConnectionManagerImpl;
import org.babyfish.jimmer.sql.example.database.DatabaseInitializer;
import org.babyfish.jimmer.sql.example.database.TransactionManager;
import org.babyfish.jimmer.sql.example.filter.FileFilter;
import org.babyfish.jimmer.sql.example.service.FileService;
import org.babyfish.jimmer.sql.example.service.UserService;

public interface Context {

    JSqlClient SQL_CLIENT =
            new AbstractJSqlClientWrapper() {
                @Override
                protected Builder createBuilder() {
                    return JSqlClient.newBuilder()
                            .setDialect(new PostgresDialect())
                            .setConnectionManager(new ConnectionManagerImpl())
                            .setTriggerType(TriggerType.TRANSACTION_ONLY)
                            .setCacheFactory(new CacheFactoryImpl())
                            .addFilters(RESOURCE_FILTER)
                            .addInitializers(new DatabaseInitializer());
                }
            };

    UserService USER_SERVICE = new UserService();

    FileService FILE_SERVICE = new FileService();

    FileFilter RESOURCE_FILTER = new FileFilter();

    TransactionManager TRANSACTION_MANAGER = new TransactionManager();

    CommandDispatcher COMMAND_DISPATCHER = new CommandDispatcher(

            new WhoAmI(),
            new Users(),
            new Login(),
            new Logout(),

            new Tree(),
            new MkDir(),
            new Touch(),
            new Rm(),

            new Grant(),
            new Revoke(),

            new TraceCache(),
            new ClearCache()
    );

    CacheStorage CACHE_STORAGE = new CacheStorage();
}
