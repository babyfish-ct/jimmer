package org.babyfish.jimmer.sql.example;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.di.JLazyInitializationSqlClient;
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
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.Collection;

public interface Context {

    JSqlClient SQL_CLIENT =
            new JLazyInitializationSqlClient() {
                @Override
                protected Builder createBuilder() {
                    return JSqlClient.newBuilder()
                            .setDialect(new PostgresDialect())
                            .setConnectionManager(new ConnectionManagerImpl())
                            .setExecutor(Executor.log())
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
            new Mv(),
            new Rm(),

            new Grant(),
            new Revoke(),

            new Cache(),
            new DCache()
    );

    CacheStorage CACHE_STORAGE = new CacheStorage();

    static void initialize() {
        TRANSACTION_MANAGER.execute(() -> {
            ((JSqlClientImplementor)SQL_CLIENT).initialize();
           return null;
        });
    }
}
